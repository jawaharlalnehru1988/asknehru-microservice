package com.asknehru.auth.controller;

import com.asknehru.auth.dto.LoginRequest;
import com.asknehru.auth.dto.RegisterRequest;
import com.asknehru.auth.dto.RefreshRequest;
import com.asknehru.auth.service.AuthService;
import com.asknehru.auth.repository.UserAccountRepository;
import com.asknehru.auth.model.UserAccount;
import com.asknehru.contracts.auth.AuthTokenResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CompatibilityAuthController {

    private final AuthService authService;
    private final UserAccountRepository userAccountRepository;

    public CompatibilityAuthController(AuthService authService, UserAccountRepository userAccountRepository) {
        this.authService = authService;
        this.userAccountRepository = userAccountRepository;
    }

    // Compatibility login mapping to match django rest framework token endpoint
    @PostMapping({"/api/auth/token", "/api/auth/token/"})
    public ResponseEntity<DjangoTokenResponse> login(@Valid @RequestBody DjangoLoginRequest request) {
        AuthTokenResponse tokens = authService.login(new LoginRequest(request.username(), request.password()));
        return ResponseEntity.ok(new DjangoTokenResponse(tokens.accessToken(), tokens.refreshToken()));
    }

    // Compatibility refresh mapping to match django rest framework refresh token endpoint
    @PostMapping({"/api/auth/token/refresh", "/api/auth/token/refresh/"})
    public ResponseEntity<DjangoRefreshResponse> refresh(@Valid @RequestBody DjangoRefreshRequest request) {
        AuthTokenResponse tokens = authService.refresh(new RefreshRequest(request.refresh()));
        return ResponseEntity.ok(new DjangoRefreshResponse(tokens.accessToken()));
    }

    // Compatibility register mapping to match django rest framework registration endpoint
    @PostMapping({"/api/interview/auth/register", "/api/interview/auth/register/"})
    public ResponseEntity<DjangoRegisterResponse> register(@Valid @RequestBody DjangoRegisterRequest request) {
        String email = request.email() != null && !request.email().isBlank() ? request.email() : request.username();
        if (!email.contains("@")) {
            email = email + "@asknehru.local";
        }

        authService.register(new RegisterRequest(
                email,
                request.password(),
                request.username(),
                null
        ));

        // Retrieve the created user account to get the auto-generated ID
        UserAccount user = userAccountRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new RuntimeException("Failed to retrieve registered user"));

        return ResponseEntity.ok(new DjangoRegisterResponse(user.getId(), user.getEmail()));
    }

    public record DjangoLoginRequest(String username, String password) {}
    public record DjangoTokenResponse(String access, String refresh) {}
    public record DjangoRefreshRequest(String refresh) {}
    public record DjangoRefreshResponse(String access) {}
    public record DjangoRegisterRequest(String username, String email, String password) {}
    public record DjangoRegisterResponse(Long id, String username) {}
}
