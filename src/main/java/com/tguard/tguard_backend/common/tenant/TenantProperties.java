package com.tguard.tguard_backend.common.tenant;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tguard.tenancy")
public record TenantProperties(
        String header,
        String defaultTenant
) {
    public String headerOrDefault() {
        if (header != null && !header.isBlank()) {
            return header;
        }
        return "X-Tenant-Id";
    }

    public String defaultTenantOr(String fallback) {
        if (defaultTenant != null && !defaultTenant.isBlank()) {
            return defaultTenant;
        }
        return fallback;
    }
}
