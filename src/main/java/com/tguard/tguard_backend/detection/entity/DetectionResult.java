package com.tguard.tguard_backend.detection.entity;

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

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @Column(nullable = false)
    private Boolean isSuspicious;

    private String reason;

    @Column(nullable = false)
    private LocalDateTime detectedAt;

    @Builder
    private DetectionResult(Transaction transaction, Boolean isSuspicious, String reason, LocalDateTime detectedAt) {
        this.transaction = transaction;
        this.isSuspicious = isSuspicious;
        this.reason = reason;
        this.detectedAt = detectedAt;
    }
}
