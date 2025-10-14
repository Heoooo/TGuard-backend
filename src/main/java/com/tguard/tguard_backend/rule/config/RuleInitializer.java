package com.tguard.tguard_backend.rule.config;

import com.tguard.tguard_backend.common.tenant.TenantProperties;
import com.tguard.tguard_backend.rule.entity.Rule;
import com.tguard.tguard_backend.rule.entity.RuleType;
import com.tguard.tguard_backend.rule.repository.RuleRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RuleInitializer {

    private final RuleRepository ruleRepository;
    private final TenantProperties tenantProperties;

    @PostConstruct
    public void initDefaultRules() {
        String tenantId = tenantProperties.defaultTenantOr("default");
        if (!ruleRepository.findByTenantId(tenantId).isEmpty()) {
            return;
        }

        ruleRepository.save(Rule.builder()
                .tenantId(tenantId)
                .ruleName("High Amount Rule")
                .description("Flag transactions above 1,000,000 KRW")
                .type(RuleType.AMOUNT)
                .active(true)
                .build());

        ruleRepository.save(Rule.builder()
                .tenantId(tenantId)
                .ruleName("Geo-Location Change Rule")
                .description("Flag transactions with location change within 10 minutes")
                .type(RuleType.LOCATION)
                .active(true)
                .build());

        ruleRepository.save(Rule.builder()
                .tenantId(tenantId)
                .ruleName("Abroad Transaction Rule")
                .description("Flag transactions occurring outside the registered country")
                .type(RuleType.LOCATION)
                .active(true)
                .build());

        ruleRepository.save(Rule.builder()
                .tenantId(tenantId)
                .ruleName("Night Time Transaction Rule")
                .description("Flag transactions occurring between 01:00 and 05:00")
                .type(RuleType.PATTERN)
                .active(false)
                .build());

        ruleRepository.save(Rule.builder()
                .tenantId(tenantId)
                .ruleName("Device Change Rule")
                .description("Flag transactions from a different device within 30 minutes")
                .type(RuleType.DEVICE)
                .active(true)
                .build());

        ruleRepository.save(Rule.builder()
                .tenantId(tenantId)
                .ruleName("Rapid Sequential Transactions Rule")
                .description("Flag three or more transactions within one minute")
                .type(RuleType.PATTERN)
                .active(true)
                .build());
    }
}
