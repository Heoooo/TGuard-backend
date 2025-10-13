package com.tguard.tguard_backend.detection.entity;

import com.tguard.tguard_backend.rule.entity.Rule;
import com.tguard.tguard_backend.transaction.entity.Transaction;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(indexes = @Index(name = "idx_detection_tenant", columnList = "tenant_id"))
public class DetectionResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rule_id")
    private Rule rule;

    @Column(nullable = false)
    private boolean suspicious;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int riskScore;

    @Column(nullable = false, columnDefinition = "double precision default 0")
    private double probability;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime detectedAt;

    @PrePersist
    public void prePersist() {
        this.detectedAt = LocalDateTime.now();
    }

    @Builder
    public DetectionResult(String tenantId, Transaction transaction, Rule rule, boolean suspicious, int riskScore, double probability, String reason) {
        this.tenantId = tenantId;
        this.transaction = transaction;
        this.rule = rule;
        this.suspicious = suspicious;
        this.riskScore = riskScore;
        this.probability = probability;
        this.reason = reason;
    }
}
