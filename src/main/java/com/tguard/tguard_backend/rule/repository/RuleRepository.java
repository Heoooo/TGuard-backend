package com.tguard.tguard_backend.rule.repository;

import com.tguard.tguard_backend.rule.entity.Rule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RuleRepository extends JpaRepository<Rule, Long> {
    boolean existsByTenantIdAndRuleName(String tenantId, String ruleName);
    List<Rule> findByTenantId(String tenantId);
    List<Rule> findByTenantIdAndActiveTrue(String tenantId);
    Optional<Rule> findByIdAndTenantId(Long id, String tenantId);
}
