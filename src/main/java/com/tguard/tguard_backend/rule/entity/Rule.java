package com.tguard.tguard_backend.rule.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "rule",
        uniqueConstraints = @UniqueConstraint(name = "uk_rule_tenant_name", columnNames = {"tenant_id", "ruleName"}),
        indexes = @Index(name = "idx_rule_tenant", columnList = "tenant_id"))
public class Rule {
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    public void setDescription(String description) { this.description = description; }
    public void setActive(boolean active) { this.active = active; }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(nullable = false)
    private String ruleName;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RuleType type;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    private Rule(String tenantId, String ruleName, String description, RuleType type, boolean active) {
        this.tenantId = tenantId;
        this.ruleName = ruleName;
        this.description = description;
        this.type = type;
        this.active = active;
    }

    public void toggleActive() {
        this.active = !this.active;
    }

    public void updateDescription(String description) {
        this.description = description;
    }
}
