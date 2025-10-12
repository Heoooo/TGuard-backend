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

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "daily_transaction_report",
        uniqueConstraints = @jakarta.persistence.UniqueConstraint(name = "uk_daily_report_tenant_date", columnNames = {"tenant_id", "report_date"}),
        indexes = @Index(name = "idx_daily_report_tenant", columnList = "tenant_id"))
public class DailyTransactionReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "transaction_count", nullable = false)
    private int transactionCount;

    @Column(name = "total_amount", nullable = false)
    private double totalAmount;

    @Column(name = "average_amount", nullable = false)
    private double averageAmount;

    @Column(name = "dominant_channel")
    private String dominantChannel;

    @Column(name = "window_start", nullable = false)
    private LocalDateTime windowStart;

    @Column(name = "window_end", nullable = false)
    private LocalDateTime windowEnd;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
