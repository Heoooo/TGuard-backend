package com.tguard.tguard_backend.blockeduser.dto;

import jakarta.validation.constraints.NotBlank;

public record BlockedUserRequest(
        @NotBlank String reason
) {
}
