package com.tguard.tguard_backend.kafka.repository;

import com.tguard.tguard_backend.kafka.entity.DlqTransactionRetry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DlqTransactionRetryRepository extends JpaRepository<DlqTransactionRetry, Long> {

    Optional<DlqTransactionRetry> findByTenantIdAndTransactionId(String tenantId, Long transactionId);

    List<DlqTransactionRetry> findTop50ByNextRetryAtBeforeOrderByNextRetryAtAsc(LocalDateTime time);

    long countByTenantIdAndNextRetryAtBefore(String tenantId, LocalDateTime time);

    @Query("select r.tenantId, count(r) from DlqTransactionRetry r group by r.tenantId")
    List<Object[]> countPendingByTenant();
}
