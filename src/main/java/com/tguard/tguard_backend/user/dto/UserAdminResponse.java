package com.tguard.tguard_backend.user.dto;

import java.time.LocalDateTime;

public record UserAdminResponse(
        Long id,
        String username,
        String tenantId,
        String role,
        String phoneNumber,
        String status,
        LocalDateTime createdAt
) {
}
