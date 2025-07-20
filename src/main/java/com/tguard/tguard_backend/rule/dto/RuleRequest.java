package com.tguard.tguard_backend.rule.dto;

import jakarta.validation.constraints.NotBlank;

public record RuleRequest(
        @NotBlank String ruleName,
        @NotBlank String condition,
        @NotBlank String value
) {
}
