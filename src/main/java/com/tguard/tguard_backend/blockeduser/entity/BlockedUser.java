package com.tguard.tguard_backend.blockeduser.entity;

import com.tguard.tguard_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BlockedUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime blockedAt;

    @PrePersist
    public void prePersist() {
        this.blockedAt = LocalDateTime.now();
    }

    @Builder
    private BlockedUser(User user, String reason) {
        this.user = user;
        this.reason = reason;
    }
}
