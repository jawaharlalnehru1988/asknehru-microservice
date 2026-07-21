package com.asknehru.auth.security;

import java.security.Principal;

public record AuthPrincipal(
        Long userId,
        String email
) implements Principal {
    @Override
    public String getName() {
        return String.valueOf(userId);
    }
}