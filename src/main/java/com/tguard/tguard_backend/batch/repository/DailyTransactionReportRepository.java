package com.tguard.tguard_backend.batch.repository;

import com.tguard.tguard_backend.batch.entity.DailyTransactionReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface DailyTransactionReportRepository extends JpaRepository<DailyTransactionReport, Long> {

    boolean existsByTenantIdAndReportDate(String tenantId, LocalDate reportDate);
}
