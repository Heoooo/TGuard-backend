package com.tguard.tguard_backend.transaction;

import com.tguard.tguard_backend.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private Double amount;

    private String location;
    private String deviceInfo;

    @Column(nullable = false)
    private LocalDateTime transactionTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    public enum Status {
        PENDING, APPROVED, REJECTED
    }

    @Builder
    private Transaction(User user, Double amount, String location, String deviceInfo,
                        LocalDateTime transactionTime, Status status) {
        this.user = user;
        this.amount = amount;
        this.location = location;
        this.deviceInfo = deviceInfo;
        this.transactionTime = transactionTime;
        this.status = status;
    }
}
