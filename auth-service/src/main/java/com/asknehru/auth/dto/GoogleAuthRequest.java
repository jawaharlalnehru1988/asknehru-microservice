package com.asknehru.auth.dto;

public record GoogleAuthRequest(
        String credential,
        String email,
        String name,
        String picture
) {
}
