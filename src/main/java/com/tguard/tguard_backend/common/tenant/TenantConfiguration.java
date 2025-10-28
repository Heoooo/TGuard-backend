package com.tguard.tguard_backend.common.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tguard.tguard_backend.tenant.service.TenantService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TenantProperties.class)
public class TenantConfiguration {

    @Bean
    public TenantContextFilter tenantContextFilter(TenantProperties tenantProperties,
                                                   TenantService tenantService,
                                                   ObjectMapper objectMapper) {
        return new TenantContextFilter(tenantProperties, tenantService, objectMapper);
    }
}
