package com.tguard.tguard_backend.detection.repository;

import com.tguard.tguard_backend.detection.entity.DetectionResult;
import com.tguard.tguard_backend.transaction.entity.Transaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DetectionResultRepository extends JpaRepository<DetectionResult, Long> {
    Optional<DetectionResult> findByTransaction(Transaction transaction);

    @Query("select avg(dr.riskScore) from DetectionResult dr where dr.tenantId = :tenantId")
    Optional<Double> averageRiskScore(@Param("tenantId") String tenantId);

    @Query("select avg(dr.probability) from DetectionResult dr where dr.tenantId = :tenantId")
    Optional<Double> averageProbability(@Param("tenantId") String tenantId);

    @Query("select dr.rule.ruleName, count(dr) from DetectionResult dr where dr.tenantId = :tenantId group by dr.rule.ruleName order by count(dr) desc")
    List<Object[]> countByRule(@Param("tenantId") String tenantId, Pageable pageable);

    List<DetectionResult> findTop10ByTenantIdOrderByDetectedAtDesc(String tenantId);
    List<DetectionResult> findByTenantIdOrderByDetectedAtDesc(String tenantId);
    Optional<DetectionResult> findByIdAndTenantId(Long id, String tenantId);

    long countByTenantId(String tenantId);
}
