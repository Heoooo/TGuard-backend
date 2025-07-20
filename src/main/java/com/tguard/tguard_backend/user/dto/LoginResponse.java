package com.tguard.tguard_backend.user.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken
) {
}
