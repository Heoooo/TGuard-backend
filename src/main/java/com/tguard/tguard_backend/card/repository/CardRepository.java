package com.tguard.tguard_backend.card.repository;

import com.tguard.tguard_backend.card.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {
    boolean existsByOwner_TenantIdAndOwner_UsernameAndBrandIgnoreCaseAndLast4(String tenantId, String ownerUsername, String brand, String last4);
    List<Card> findByOwner_TenantIdAndOwner_Username(String tenantId, String ownerUsername);
    Optional<Card> findByIdAndOwner_TenantIdAndOwner_Username(Long id, String tenantId, String ownerUsername);
    Optional<Card> findByOwner_TenantIdAndBrandIgnoreCaseAndLast4(String tenantId, String brand, String last4);
    Optional<Card> findByOwner_TenantIdAndLast4(String tenantId, String last4);
}
