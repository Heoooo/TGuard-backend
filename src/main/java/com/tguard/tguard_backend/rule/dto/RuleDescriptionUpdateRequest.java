package com.tguard.tguard_backend.rule.dto;

import jakarta.validation.constraints.NotBlank;

public record RuleDescriptionUpdateRequest(@NotBlank String description) {
}
