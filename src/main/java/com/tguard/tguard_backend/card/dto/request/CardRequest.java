package com.tguard.tguard_backend.card.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CardRequest(
        @NotBlank String brand,
        @NotBlank String last4,
        String nickname
) {}
