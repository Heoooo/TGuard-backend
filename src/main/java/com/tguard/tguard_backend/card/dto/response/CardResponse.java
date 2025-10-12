package com.tguard.tguard_backend.card.dto.response;

public record CardResponse(
        Long id, String brand, String last4, String nickname
) {}
