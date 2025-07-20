package com.tguard.tguard_backend.user.dto;

public record UserProfileResponse(
        Long id,
        String email,
        String name,
        String phoneNumber
) {
}
