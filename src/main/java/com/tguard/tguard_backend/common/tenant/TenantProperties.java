package com.tguard.tguard_backend.common.tenant;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tguard.tenancy")
public record TenantProperties(
        String header,
        String defaultTenant
) {
    public String headerOrDefault() {
        return header != null && !header.isBlank() ? header : "X-Tenant-Id";
    }

    public String defaultTenantOr(String fallback) {
        return defaultTenant != null && !defaultTenant.isBlank() ? defaultTenant : fallback;
    }
}
