package com.tguard.tguard_backend.transaction.entity;

import com.tguard.tguard_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tx_external_event", columnNames = {"tenant_id", "externalEventId"}),
                @UniqueConstraint(name = "uk_tx_payment_key", columnNames = {"tenant_id", "paymentKey"}),
                @UniqueConstraint(name = "uk_tx_order_id", columnNames = {"tenant_id", "orderId"})
        },
        indexes = {
                @Index(name = "idx_tx_tenant_time", columnList = "tenant_id,transactionTime"),
                @Index(name = "idx_tx_user_time", columnList = "user_id,transactionTime")
        }
)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private Double amount;

    private String location;

    private String deviceInfo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Channel channel;

    @Column(nullable = false)
    private LocalDateTime transactionTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column
    private String externalEventId;

    private String paymentKey;
    private String orderId;

    private String currency;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String rawPayload;

    @Builder
    private Transaction(
            String tenantId,
            User user,
            Double amount,
            String location,
            String deviceInfo,
            LocalDateTime transactionTime,
            Status status,
            Channel channel,
            String externalEventId,
            String paymentKey,
            String orderId,
            String currency,
            String rawPayload
    ) {
        this.tenantId = tenantId;
        this.user = user;
        this.amount = amount;
        this.location = location;
        this.deviceInfo = deviceInfo;
        this.transactionTime = transactionTime;
        this.status = status;
        this.channel = channel;
        this.externalEventId = externalEventId;
        this.paymentKey = paymentKey;
        this.orderId = orderId;
        this.currency = currency;
        this.rawPayload = rawPayload;
    }

    public enum Status { PENDING, APPROVED, REJECTED }

    public void updateStatus(Status status) {
        this.status = status;
    }
}
