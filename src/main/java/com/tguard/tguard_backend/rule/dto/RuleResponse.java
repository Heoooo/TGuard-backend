package com.tguard.tguard_backend.rule.dto;

import com.tguard.tguard_backend.rule.entity.RuleType;

import java.time.LocalDateTime;

public record RuleResponse(
        Long id,
        String ruleName,
        String description,
        RuleType type,
        boolean active,
        LocalDateTime createdAt
) {
}
