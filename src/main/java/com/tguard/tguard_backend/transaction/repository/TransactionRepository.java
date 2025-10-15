package com.tguard.tguard_backend.transaction.repository;

import com.tguard.tguard_backend.transaction.entity.Transaction;
import com.tguard.tguard_backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByTenantIdAndUser(String tenantId, User user);
    Optional<Transaction> findByTenantIdAndExternalEventId(String tenantId, String externalEventId);
    Optional<Transaction> findByTenantIdAndPaymentKey(String tenantId, String paymentKey);
    Optional<Transaction> findByTenantIdAndOrderId(String tenantId, String orderId);
    List<Transaction> findTop10ByTenantIdAndUserOrderByTransactionTimeDesc(String tenantId, User user);
    Optional<Transaction> findByIdAndTenantId(Long id, String tenantId);

    @Query("select t.tenantId, max(t.transactionTime) from Transaction t group by t.tenantId")
    List<Object[]> findLatestTransactionPerTenant();

    @Query("select t.tenantId, count(t) from Transaction t where t.transactionTime >= :since group by t.tenantId")
    List<Object[]> countTransactionsSince(@Param("since") LocalDateTime since);
}
