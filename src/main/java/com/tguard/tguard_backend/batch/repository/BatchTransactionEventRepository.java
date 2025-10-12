package com.tguard.tguard_backend.batch.repository;

import com.tguard.tguard_backend.batch.entity.BatchTransactionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BatchTransactionEventRepository extends JpaRepository<BatchTransactionEvent, Long> {

    List<BatchTransactionEvent> findByTenantIdAndProcessedFalseAndTransactionTimeBetween(String tenantId, LocalDateTime start, LocalDateTime end);

    List<BatchTransactionEvent> findByProcessedFalseAndTransactionTimeBetween(LocalDateTime start, LocalDateTime end);

    long countByTenantIdAndProcessedFalse(String tenantId);

    Optional<BatchTransactionEvent> findTop1ByTenantIdAndProcessedFalseOrderByTransactionTimeAsc(String tenantId);

    void deleteByTenantIdAndProcessedTrueAndProcessedAtBefore(String tenantId, LocalDateTime before);

    @Query("select e.tenantId, count(e) from BatchTransactionEvent e where e.processed = false group by e.tenantId")
    List<Object[]> countPendingByTenant();

    @Query("select distinct e.tenantId from BatchTransactionEvent e")
    List<String> findDistinctTenantIds();
}
