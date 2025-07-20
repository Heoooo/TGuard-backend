package com.tguard.tguard_backend.blockeduser.dto;

import java.time.LocalDateTime;

public record BlockedUserResponse(
        Long id,
        Long userId,
        String reason,
        LocalDateTime blockedAt
) {
}
