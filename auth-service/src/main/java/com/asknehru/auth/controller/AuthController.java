package com.asknehru.auth.controller;

import com.asknehru.auth.dto.LoginRequest;
import com.asknehru.auth.dto.LogoutRequest;
import com.asknehru.auth.dto.RefreshRequest;
import com.asknehru.auth.dto.RegisterRequest;
import com.asknehru.auth.security.AuthPrincipal;
import com.asknehru.auth.service.AuthService;
import com.asknehru.contracts.api.ApiSuccessResponse;
import com.asknehru.contracts.auth.AuthMeResponse;
import com.asknehru.contracts.auth.AuthTokenResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiSuccessResponse<AuthTokenResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        AuthTokenResponse tokens = authService.register(request);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Registration successful", tokens));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiSuccessResponse<AuthTokenResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        AuthTokenResponse tokens = authService.login(request);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Login successful", tokens));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiSuccessResponse<AuthTokenResponse>> refresh(
            @Valid @RequestBody RefreshRequest request
    ) {
        AuthTokenResponse tokens = authService.refresh(request);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Token refreshed", tokens));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiSuccessResponse<Void>> logout(
            @Valid @RequestBody LogoutRequest request
    ) {
        authService.logout(request);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Logout successful", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiSuccessResponse<AuthMeResponse>> me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal principal)) {
            return ResponseEntity.status(401).build();
        }
        AuthMeResponse payload = new AuthMeResponse(principal.userId(), principal.email());
        return ResponseEntity.ok(new ApiSuccessResponse<>("Current user fetched", payload));
    }

    @GetMapping("/admin/ping")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<String>> adminPing() {
        return ResponseEntity.ok(new ApiSuccessResponse<>("Admin access granted", "pong"));
    }
}
