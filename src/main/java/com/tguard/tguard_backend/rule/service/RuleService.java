package com.tguard.tguard_backend.rule.service;

import com.tguard.tguard_backend.rule.dto.RuleRequest;
import com.tguard.tguard_backend.rule.dto.RuleResponse;
import com.tguard.tguard_backend.rule.entity.Rule;
import com.tguard.tguard_backend.rule.exception.RuleNotFoundException;
import com.tguard.tguard_backend.rule.repository.RuleRepository;
import com.tguard.tguard_backend.transaction.entity.Transaction;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RuleService {

    private final RuleRepository ruleRepository;

    @Transactional
    public List<Rule> getAllRules() {
        return ruleRepository.findAll();
    }

    public Rule getRule(Long id) {
        return ruleRepository.findById(id)
                .orElseThrow(RuleNotFoundException::new);
    }

    @Transactional
    public RuleResponse createRule(RuleRequest request) {
        Rule rule = Rule.builder()
                .ruleName(request.ruleName())
                .condition(request.condition())
                .value(request.value())
                .build();

        ruleRepository.save(rule);

        return toResponse(rule);
    }

    @Transactional
    public RuleResponse updateRule(Long id, RuleRequest request) {
        Rule rule = getRule(id);
        rule.update(request.ruleName(), request.condition(), request.value());
        return toResponse(rule);
    }

    @Transactional
    public void deleteRule(Long id) {
        Rule rule = getRule(id);
        ruleRepository.delete(rule);
    }

    public RuleResponse toResponse(Rule rule) {
        return new RuleResponse(
                rule.getId(),
                rule.getRuleName(),
                rule.getCondition(),
                rule.getValue(),
                rule.getCreatedAt()
        );
    }

    public boolean isSuspicious(Transaction tx) {
        List<Rule> rules = getAllRules();

        for (Rule rule : rules) {
            String condition = rule.getCondition();
            String value = rule.getValue();

            if (condition.equalsIgnoreCase("amount >")) {
                try {
                    double threshold = Double.parseDouble(value);
                    if (tx.getAmount() != null && tx.getAmount() > threshold) {
                        return true;
                    }
                } catch (NumberFormatException e) {
                    // 잘못된 값은 무시
                }
            }

        }

        return false;
    }
}
