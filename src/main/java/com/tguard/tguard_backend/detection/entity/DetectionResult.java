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
public class DetectionResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rule_id")
    private Rule rule;

    @Column(nullable = false)
    private boolean suspicious;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime detectedAt;

    @PrePersist
    public void prePersist() {
        this.detectedAt = LocalDateTime.now();
    }

    @Builder
    public DetectionResult(Transaction transaction, Rule rule, boolean suspicious, String reason) {
        this.transaction = transaction;
        this.rule = rule;
        this.suspicious = suspicious;
        this.reason = reason;
    }
}
