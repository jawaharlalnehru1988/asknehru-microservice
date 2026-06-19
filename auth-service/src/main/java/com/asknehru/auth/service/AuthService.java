package com.asknehru.auth.service;

import com.asknehru.auth.dto.LoginRequest;
import com.asknehru.auth.dto.LogoutRequest;
import com.asknehru.auth.dto.RefreshRequest;
import com.asknehru.auth.dto.RegisterRequest;
import com.asknehru.auth.exception.AuthConflictException;
import com.asknehru.auth.exception.AuthUnauthorizedException;
import com.asknehru.auth.model.Role;
import com.asknehru.auth.model.RefreshToken;
import com.asknehru.auth.model.UserAccount;
import com.asknehru.auth.model.UserRole;
import com.asknehru.auth.repository.RefreshTokenRepository;
import com.asknehru.auth.repository.RoleRepository;
import com.asknehru.auth.repository.UserAccountRepository;
import com.asknehru.auth.repository.UserRoleRepository;
import com.asknehru.contracts.auth.AuthTokenResponse;
import com.asknehru.contracts.events.UserRegisteredEvent;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.kafka.core.KafkaTemplate;

@Service
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;

    public AuthService(
            UserAccountRepository userAccountRepository,
            RefreshTokenRepository refreshTokenRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate
    ) {
        this.userAccountRepository = userAccountRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public AuthTokenResponse register(RegisterRequest request) {
        String normalizedEmail = request.email() == null ? "" : request.email().trim().toLowerCase();

        if (userAccountRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new AuthConflictException("Email is already registered");
        }

        UserAccount user = new UserAccount();
        user.setEmail(normalizedEmail);
        user.setFullName(request.fullName().trim());
        user.setPhoneNumber(request.phoneNumber() != null ? request.phoneNumber().trim() : null);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setEnabled(true);
        user.setAccountLocked(false);
        user.setEmailVerified(true);

        UserAccount createdUser = userAccountRepository.save(user);
        ensureDefaultRole(createdUser.getId());

        String tokenId = UUID.randomUUID().toString();
        String accessToken = jwtTokenService.generateAccessToken(createdUser);
        String refreshTokenValue = jwtTokenService.generateRefreshToken(createdUser, tokenId);
        persistRefreshToken(createdUser.getId(), tokenId, jwtTokenService.getRefreshExpirySeconds());

        UserRegisteredEvent event = new UserRegisteredEvent(createdUser.getId(), createdUser.getEmail(), createdUser.getFullName());
        kafkaTemplate.send("user-registration-topic", createdUser.getId().toString(), event);

        return new AuthTokenResponse(
                accessToken,
                refreshTokenValue,
                "Bearer",
                jwtTokenService.getAccessExpirySeconds()
        );
    }

    @Transactional
    public AuthTokenResponse login(LoginRequest request) {
        String normalizedEmail = request.email() == null ? "" : request.email().trim().toLowerCase();

        UserAccount user = userAccountRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new AuthUnauthorizedException("Invalid credentials"));

        if (!user.isEnabled()) {
            throw new AuthUnauthorizedException("Account is disabled");
        }

        if (user.isAccountLocked()) {
            throw new AuthUnauthorizedException("Account is locked");
        }

        if (!user.isEmailVerified()) {
            throw new AuthUnauthorizedException("Email is not verified");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AuthUnauthorizedException("Invalid credentials");
        }

        ensureDefaultRole(user.getId());

        String tokenId = UUID.randomUUID().toString();
        String accessToken = jwtTokenService.generateAccessToken(user);
        String refreshTokenValue = jwtTokenService.generateRefreshToken(user, tokenId);

        persistRefreshToken(user.getId(), tokenId, jwtTokenService.getRefreshExpirySeconds());

        return new AuthTokenResponse(
                accessToken,
                refreshTokenValue,
                "Bearer",
                jwtTokenService.getAccessExpirySeconds()
        );
    }

    @Transactional
    public void logout(LogoutRequest request) {
        Claims claims;
        try {
            claims = jwtTokenService.parseAndValidate(request.refreshToken());
        } catch (JwtException ex) {
            throw new AuthUnauthorizedException("Invalid refresh token");
        }

        String tokenType = claims.get("type", String.class);
        if (!"refresh".equals(tokenType)) {
            throw new AuthUnauthorizedException("Invalid token type");
        }

        String tokenId = claims.getId();
        if (tokenId == null || tokenId.isBlank()) {
            throw new AuthUnauthorizedException("Invalid refresh token id");
        }

        refreshTokenRepository.findByTokenIdAndRevokedAtIsNull(tokenId).ifPresent(token -> {
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
        });
    }

    @Transactional
    public AuthTokenResponse refresh(RefreshRequest request) {
        Claims claims;
        try {
            claims = jwtTokenService.parseAndValidate(request.refreshToken());
        } catch (JwtException ex) {
            throw new AuthUnauthorizedException("Invalid refresh token");
        }

        String tokenType = claims.get("type", String.class);
        if (!"refresh".equals(tokenType)) {
            throw new AuthUnauthorizedException("Invalid token type");
        }

        String tokenId = claims.getId();
        if (tokenId == null || tokenId.isBlank()) {
            throw new AuthUnauthorizedException("Invalid refresh token id");
        }

        RefreshToken storedToken = refreshTokenRepository.findByTokenIdAndRevokedAtIsNull(tokenId)
                .orElseThrow(() -> new AuthUnauthorizedException("Refresh token not found or revoked"));

        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            throw new AuthUnauthorizedException("Refresh token expired");
        }

        UserAccount user = userAccountRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new AuthUnauthorizedException("User not found for token"));

        storedToken.setRevokedAt(Instant.now());
        refreshTokenRepository.save(storedToken);

        String newTokenId = UUID.randomUUID().toString();
        String accessToken = jwtTokenService.generateAccessToken(user);
        String refreshTokenValue = jwtTokenService.generateRefreshToken(user, newTokenId);

        persistRefreshToken(user.getId(), newTokenId, jwtTokenService.getRefreshExpirySeconds());

        return new AuthTokenResponse(
                accessToken,
                refreshTokenValue,
                "Bearer",
                jwtTokenService.getAccessExpirySeconds()
        );
    }

    private void persistRefreshToken(Long userId, String tokenId, long refreshTtlSeconds) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(userId);
        refreshToken.setTokenId(tokenId);
        refreshToken.setExpiresAt(Instant.now().plusSeconds(refreshTtlSeconds));
        refreshTokenRepository.save(refreshToken);
    }

    private void ensureDefaultRole(Long userId) {
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("ROLE_USER");
                    return roleRepository.save(role);
                });

        if (userRoleRepository.existsByUserIdAndRoleId(userId, userRole.getId())) {
            return;
        }

        UserRole mapping = new UserRole();
        mapping.setUserId(userId);
        mapping.setRoleId(userRole.getId());
        userRoleRepository.save(mapping);
    }
}
