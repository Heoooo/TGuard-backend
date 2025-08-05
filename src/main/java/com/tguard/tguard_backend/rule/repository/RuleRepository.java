package com.tguard.tguard_backend.rule.repository;

import com.tguard.tguard_backend.rule.entity.Rule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RuleRepository extends JpaRepository<Rule, Long> {
    boolean existsByRuleName(String ruleName);
}