package com.tguard.tguard_backend.rule.config;

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

    @PostConstruct
    public void initDefaultRules() {
        if (ruleRepository.count() == 0) {
            ruleRepository.save(Rule.builder()
                    .ruleName("High Amount Rule")
                    .description("거래 금액이 1,000,000원 초과 시 탐지")
                    .type(RuleType.AMOUNT)
                    .active(true)
                    .build());

            ruleRepository.save(Rule.builder()
                    .ruleName("Geo-Location Change Rule")
                    .description("10분 내 위치 변경 거래 탐지")
                    .type(RuleType.PATTERN)
                    .active(true)
                    .build());

            ruleRepository.save(Rule.builder()
                    .ruleName("Abroad Transaction Rule")
                    .description("국내 이외 지역에서 거래 시 탐지")
                    .type(RuleType.LOCATION)
                    .active(true)
                    .build());

            ruleRepository.save(Rule.builder()
                    .ruleName("Night Time Transaction Rule")
                    .description("새벽 시간대(01:00~05:00) 거래 탐지")
                    .type(RuleType.PATTERN)
                    .active(false) // 기본 비활성화
                    .build());
        }
    }
}
