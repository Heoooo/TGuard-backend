package com.tguard.tguard_backend.tenant.config;

import com.tguard.tguard_backend.common.tenant.TenantProperties;
import com.tguard.tguard_backend.tenant.entity.Tenant;
import com.tguard.tguard_backend.tenant.repository.TenantRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantInitializer {

    private final TenantRepository tenantRepository;
    private final TenantProperties tenantProperties;

    @PostConstruct
    void ensureDefaultTenantExists() {
        String defaultTenant = tenantProperties.defaultTenantOr("default").trim();
        if (!tenantRepository.existsByTenantId(defaultTenant)) {
            tenantRepository.save(Tenant.builder()
                    .tenantId(defaultTenant)
                    .displayName("Default Tenant")
                    .active(true)
                    .build());
            log.info("Initialized default tenant '{}'", defaultTenant);
        }
    }
}
