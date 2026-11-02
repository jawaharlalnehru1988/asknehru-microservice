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
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    public AuthService(
            UserAccountRepository userAccountRepository,
            RefreshTokenRepository refreshTokenRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate,
            org.springframework.jdbc.core.JdbcTemplate jdbcTemplate
    ) {
        this.userAccountRepository = userAccountRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.kafkaTemplate = kafkaTemplate;
        this.jdbcTemplate = jdbcTemplate;
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
        String identifier = request.email() == null ? "" : request.email().trim().toLowerCase();

        UserAccount user = userAccountRepository.findByUsernameOrEmail(identifier, identifier)
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

    @Transactional
    public AuthTokenResponse authenticateGoogle(com.asknehru.auth.dto.GoogleAuthRequest request) {
        String email = request.email();
        String name = request.name();
        String picture = request.picture();

        if (request.credential() != null && request.credential().contains(".")) {
            try {
                String[] parts = request.credential().split("\\.");
                if (parts.length >= 2) {
                    byte[] decoded = java.util.Base64.getUrlDecoder().decode(parts[1]);
                    com.fasterxml.jackson.databind.JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(decoded);
                    if (node.hasNonNull("email")) {
                        email = node.get("email").asText();
                    }
                    if (node.hasNonNull("name")) {
                        name = node.get("name").asText();
                    }
                    if (node.hasNonNull("picture")) {
                        picture = node.get("picture").asText();
                    }
                }
            } catch (Exception ex) {
                // Ignore decoding error, fallback to passed fields
            }
        }

        if (email == null || email.isBlank()) {
            throw new AuthUnauthorizedException("Email is required for Google authentication");
        }

        String normalizedEmail = email.trim().toLowerCase();
        boolean isAdmin = "nehru00123@gmail.com".equalsIgnoreCase(normalizedEmail)
                || "jawaharlalnehruelumalai@gmail.com".equalsIgnoreCase(normalizedEmail);

        String effectiveName = (name != null && !name.isBlank()) ? name.trim() : normalizedEmail;

        UserAccount user = userAccountRepository.findByEmail(normalizedEmail)
                .orElseGet(() -> {
                    UserAccount newUser = new UserAccount();
                    newUser.setEmail(normalizedEmail);
                    newUser.setFullName(effectiveName);
                    newUser.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
                    newUser.setEnabled(true);
                    newUser.setAccountLocked(false);
                    newUser.setEmailVerified(true);
                    return userAccountRepository.save(newUser);
                });

        if (name != null && !name.isBlank() && !name.trim().equals(user.getFullName())) {
            user.setFullName(name.trim());
            user = userAccountRepository.save(user);
        }

        if (isAdmin) {
            ensureRole(user.getId(), "ROLE_ADMIN");
        }
        ensureRole(user.getId(), "ROLE_USER");

        syncToAuthUser(normalizedEmail, user.getFullName(), isAdmin);

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

    private void syncToAuthUser(String email, String fullName, boolean isAdmin) {
        try {
            java.util.List<Integer> ids = jdbcTemplate.query(
                    "SELECT id FROM auth_user WHERE LOWER(email) = ?",
                    (rs, rowNum) -> rs.getInt("id"),
                    email
            );
            if (ids.isEmpty()) {
                String baseUsername = email.contains("@") ? email.substring(0, email.indexOf("@")) : email;
                String username = baseUsername;
                int suffix = 1;
                while (true) {
                    Integer count = jdbcTemplate.queryForObject(
                            "SELECT count(*) FROM auth_user WHERE username = ?",
                            Integer.class,
                            username
                    );
                    if (count == null || count == 0) break;
                    username = baseUsername + suffix++;
                }

                jdbcTemplate.update(
                        "INSERT INTO auth_user (username, password, first_name, last_name, email, is_staff, is_active, is_superuser, date_joined) VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())",
                        username,
                        passwordEncoder.encode(UUID.randomUUID().toString()),
                        fullName != null ? fullName : "",
                        "",
                        email,
                        isAdmin,
                        true,
                        isAdmin
                );
            } else {
                jdbcTemplate.update(
                        "UPDATE auth_user SET first_name = COALESCE(NULLIF(?, ''), first_name), is_staff = ?, is_superuser = ?, is_active = true WHERE id = ?",
                        fullName != null ? fullName : "",
                        isAdmin,
                        isAdmin,
                        ids.get(0)
                );
            }
        } catch (Exception ex) {
            org.slf4j.LoggerFactory.getLogger(AuthService.class).warn("Notice syncing user to auth_user table: {}", ex.getMessage());
        }
    }

    private void ensureRole(Long userId, String roleName) {
        Role role = roleRepository.findByName(roleName)
                .orElseGet(() -> {
                    Role r = new Role();
                    r.setName(roleName);
                    return roleRepository.save(r);
                });

        if (userRoleRepository.existsByUserIdAndRoleId(userId, role.getId())) {
            return;
        }

        UserRole mapping = new UserRole();
        mapping.setUserId(userId);
        mapping.setRoleId(role.getId());
        userRoleRepository.save(mapping);
    }

    private void ensureDefaultRole(Long userId) {
        ensureRole(userId, "ROLE_USER");
    }
}
