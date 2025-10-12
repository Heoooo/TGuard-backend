package com.tguard.tguard_backend.batch.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
@Table(name = "batch_transaction_event",
        indexes = {
                @Index(name = "idx_batch_event_tenant_processed", columnList = "tenant_id,processed"),
                @Index(name = "idx_batch_event_tenant_time", columnList = "tenant_id,transaction_time")
        })
public class BatchTransactionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "amount")
    private Double amount;

    @Column(name = "location")
    private String location;

    @Column(name = "device_info")
    private String deviceInfo;

    @Column(name = "channel")
    private String channel;

    @Column(name = "transaction_time")
    private LocalDateTime transactionTime;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "processed", nullable = false)
    private boolean processed;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public void markProcessed(LocalDateTime processedAt) {
        this.processed = true;
        this.processedAt = processedAt;
    }
}
