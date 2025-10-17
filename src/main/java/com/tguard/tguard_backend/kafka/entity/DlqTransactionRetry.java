package com.tguard.tguard_backend.kafka.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dlq_transaction_retry",
        indexes = {
                @Index(name = "idx_dlq_retry_tenant", columnList = "tenant_id"),
                @Index(name = "idx_dlq_retry_next_retry", columnList = "next_retry_at")
        })
public class DlqTransactionRetry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "transaction_id")
    private Long transactionId;

    @Lob
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "error_type", columnDefinition = "TEXT")
    private String errorType;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_retry_at", nullable = false)
    private LocalDateTime nextRetryAt;

    @Column(name = "last_failed_at", nullable = false)
    private LocalDateTime lastFailedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_alerted_attempt", nullable = false)
    private int lastAlertedAttempt;

    public void refreshPayload(String payload) {
        this.payload = payload;
    }

    public void updateFailure(String errorMessage, String errorType, LocalDateTime nextRetryAt) {
        this.errorMessage = errorMessage;
        this.errorType = errorType;
        this.lastFailedAt = LocalDateTime.now();
        this.attemptCount += 1;
        this.nextRetryAt = nextRetryAt;
    }

    public void markScheduled(LocalDateTime nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    public void markAlertedUpTo(int attempt) {
        this.lastAlertedAttempt = Math.max(this.lastAlertedAttempt, attempt);
    }
}
