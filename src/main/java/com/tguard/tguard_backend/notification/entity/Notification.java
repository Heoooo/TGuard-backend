package com.tguard.tguard_backend.notification.entity;

import com.tguard.tguard_backend.transaction.entity.Transaction;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(indexes = @Index(name = "idx_notification_tenant", columnList = "tenant_id"))
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private boolean read = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    public Notification(String tenantId, String message, Transaction transaction) {
        this.tenantId = tenantId;
        this.message = message;
        this.transaction = transaction;
    }

    public void markAsRead() {
        this.read = true;
    }
}
