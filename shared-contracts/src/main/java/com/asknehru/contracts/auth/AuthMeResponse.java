package com.asknehru.contracts.auth;

public record AuthMeResponse(
        Long userId,
        String email
) {
}