package com.tguard.tguard_backend.card.dto.request;

public record CardRequest(
        String brand,
        String last4,
        Long userId
) {}
