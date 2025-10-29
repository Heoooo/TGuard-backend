package com.tguard.tguard_backend.tenant.dto;

import jakarta.validation.constraints.NotBlank;

public class TenantDtos {

    public record TenantCreateRequest(
            @NotBlank String tenantId,
            @NotBlank String displayName
    ) {
    }

    public record TenantResponse(
            Long id,
            String tenantId,
            String displayName,
            boolean active
    ) {
    }

    public record TenantValidationResponse(
            boolean valid,
            String tenantId,
            String displayName
    ) {
    }
}
