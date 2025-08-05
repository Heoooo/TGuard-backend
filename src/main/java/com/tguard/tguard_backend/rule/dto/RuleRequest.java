package com.tguard.tguard_backend.rule.dto;

import jakarta.validation.constraints.NotBlank;

public record RuleRequest(
        String description
) {
}
