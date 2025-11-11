package com.tguard.tguard_backend.rule.dto;

import com.tguard.tguard_backend.rule.entity.RuleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RuleRequest(
        @NotBlank String ruleName,
        @NotBlank String description,
        @NotNull RuleType type,
        Boolean active
) {
    public boolean activeOrDefault() {
        if (active != null) {
            return active;
        }
        return true;
    }
}
