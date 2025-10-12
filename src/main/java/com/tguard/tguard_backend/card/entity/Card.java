package com.tguard.tguard_backend.card.entity;

import com.tguard.tguard_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cards",
        uniqueConstraints = @UniqueConstraint(name = "uk_card_owner_brand_last4",
                columnNames = {"owner_id","brand","last4"}))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Card {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 32)
    private String brand;

    @Column(nullable = false, length = 4)
    private String last4;

    @Column(length = 50)
    private String nickname;
}