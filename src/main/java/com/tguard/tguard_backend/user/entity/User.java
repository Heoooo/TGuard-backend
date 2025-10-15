package com.tguard.tguard_backend.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_username_tenant", columnNames = {"username", "tenant_id"}),
                @UniqueConstraint(name = "uk_user_phone_tenant", columnNames = {"phoneNumber", "tenant_id"})
        },
        indexes = {
                @Index(name = "idx_user_tenant", columnList = "tenant_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private String role;
}
