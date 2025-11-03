package com.tguard.tguard_backend.user.dto;

public record UserProfileResponse(
        Long id,
        String username,
        String tenantId,
        String role
) {
}

