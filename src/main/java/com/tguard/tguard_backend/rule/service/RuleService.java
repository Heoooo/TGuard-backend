package com.tguard.tguard_backend.rule.service;

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
        return ruleRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public RuleResponse getRule(Long id) {
        return toResponse(ruleRepository.findById(id)
                .orElseThrow(RuleNotFoundException::new));
    }

    @Transactional
    public RuleResponse toggleRule(Long id) {
        Rule rule = ruleRepository.findById(id)
                .orElseThrow(RuleNotFoundException::new);

        rule.toggleActive();
        return toResponse(rule);
    }

    @Transactional
    public RuleResponse updateDescription(Long id, String description) {
        Rule rule = ruleRepository.findById(id)
                .orElseThrow(RuleNotFoundException::new);

        rule.updateDescription(description);
        return toResponse(rule);
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
