package com.tguard.tguard_backend.rule.dto;

import java.time.LocalDateTime;

public record RuleResponse(
        Long id,
        String ruleName,
        String condition,
        String value,
        LocalDateTime createdAt
) {
}
