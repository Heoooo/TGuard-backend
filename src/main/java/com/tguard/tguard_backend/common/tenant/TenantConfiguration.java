package com.tguard.tguard_backend.common.tenant;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TenantProperties.class)
public class TenantConfiguration {

    @Bean
    public TenantContextFilter tenantContextFilter(TenantProperties tenantProperties) {
        return new TenantContextFilter(tenantProperties);
    }
}
