package com.tguard.tguard_backend.rule.service;

import com.tguard.tguard_backend.common.tenant.TenantContextHolder;
import com.tguard.tguard_backend.rule.dto.RuleResponse;
import com.tguard.tguard_backend.rule.entity.Rule;
import com.tguard.tguard_backend.rule.exception.RuleNotFoundException;
import com.tguard.tguard_backend.rule.repository.RuleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RuleService {

    private final RuleRepository ruleRepository;

    @Transactional
    public List<RuleResponse> getAllRules() {
        String tenantId = TenantContextHolder.requireTenantId();
        return ruleRepository.findByTenantId(tenantId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public RuleResponse getRule(Long id) {
        return toResponse(getTenantRule(id));
    }

    @Transactional
    public RuleResponse toggleRule(Long id) {
        Rule rule = getTenantRule(id);
        rule.toggleActive();
        return toResponse(rule);
    }

    @Transactional
    public RuleResponse updateDescription(Long id, String description) {
        Rule rule = getTenantRule(id);
        rule.updateDescription(description);
        return toResponse(rule);
    }

    @Transactional
    public RuleResponse addRule(Rule rule) {
        String tenantId = TenantContextHolder.requireTenantId();
        if (ruleRepository.existsByTenantIdAndRuleName(tenantId, rule.getRuleName())) {
            throw new IllegalArgumentException("Rule name already exists for this tenant.");
        }
        Rule saved = ruleRepository.save(Rule.builder()
                .tenantId(tenantId)
                .ruleName(rule.getRuleName())
                .description(rule.getDescription())
                .type(rule.getType())
                .active(rule.isActive())
                .build());
        return toResponse(saved);
    }

    @Transactional
    public RuleResponse updateRule(Long id, Rule updated) {
        Rule rule = getTenantRule(id);
        rule.setRuleName(updated.getRuleName());
        rule.setDescription(updated.getDescription());
        rule.setActive(updated.isActive());
        return toResponse(rule);
    }

    @Transactional
    public void deleteRule(Long id) {
        Rule rule = getTenantRule(id);
        ruleRepository.delete(rule);
    }

    private Rule getTenantRule(Long id) {
        String tenantId = TenantContextHolder.requireTenantId();
        return ruleRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(RuleNotFoundException::new);
    }

    private RuleResponse toResponse(Rule rule) {
        return new RuleResponse(
                rule.getId(),
                rule.getRuleName(),
                rule.getDescription(),
                rule.getType(),
                rule.isActive(),
                rule.getCreatedAt()
        );
    }
}
