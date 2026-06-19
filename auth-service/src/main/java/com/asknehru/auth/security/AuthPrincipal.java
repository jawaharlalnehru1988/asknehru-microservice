package com.asknehru.auth.security;

public record AuthPrincipal(
        Long userId,
        String email
) {
}