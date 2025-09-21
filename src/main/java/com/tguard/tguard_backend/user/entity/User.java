package com.tguard.tguard_backend.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false) // 로그인 ID
    private String username;

    @Column(nullable = false)
    private String password; // BCrypt 해시

    @Column(unique = true, nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private String role; // ex) ROLE_USER
}
