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
public class Rule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String ruleName;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RuleType type; // AMOUNT, LOCATION, PATTERN, DEVICE 등

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    private Rule(String ruleName, String description, RuleType type, boolean active) {
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
