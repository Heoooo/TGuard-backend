package com.tguard.tguard_backend.card.repository;

import com.tguard.tguard_backend.card.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {
    Optional<Card> findByLast4(String last4);
    boolean existsByLast4AndUserId(String last4, Long userId);
}
