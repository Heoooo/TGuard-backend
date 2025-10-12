package com.tguard.tguard_backend.batch.service;

import com.tguard.tguard_backend.batch.entity.BatchTransactionEvent;
import com.tguard.tguard_backend.batch.entity.DailyTransactionReport;
import com.tguard.tguard_backend.batch.repository.BatchTransactionEventRepository;
import com.tguard.tguard_backend.batch.repository.DailyTransactionReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchAnalysisService {

    private final BatchTransactionEventRepository batchTransactionEventRepository;
    private final DailyTransactionReportRepository dailyTransactionReportRepository;

    @Scheduled(cron = "0 5 0 * * *")
    @Transactional
    public void generateDailyReports() {
        LocalDate targetDate = LocalDate.now().minusDays(1);
        LocalDateTime windowStart = targetDate.atStartOfDay();
        LocalDateTime windowEnd = targetDate.plusDays(1).atStartOfDay();

        List<BatchTransactionEvent> events = batchTransactionEventRepository
                .findByProcessedFalseAndTransactionTimeBetween(windowStart, windowEnd);

        if (events.isEmpty()) {
            log.debug("No batch events to process for date {}", targetDate);
            return;
        }

        Map<String, List<BatchTransactionEvent>> eventsByTenant = events.stream()
                .collect(Collectors.groupingBy(BatchTransactionEvent::getTenantId));

        eventsByTenant.forEach((tenantId, tenantEvents) -> processTenantEvents(targetDate, windowStart, windowEnd, tenantId, tenantEvents));
    }

    private void processTenantEvents(LocalDate targetDate,
                                     LocalDateTime windowStart,
                                     LocalDateTime windowEnd,
                                     String tenantId,
                                     List<BatchTransactionEvent> tenantEvents) {
        if (tenantEvents.isEmpty()) {
            return;
        }

        if (dailyTransactionReportRepository.existsByTenantIdAndReportDate(tenantId, targetDate)) {
            log.info("Daily reports already generated for tenant {} date {}, marking {} events as processed", tenantId, targetDate, tenantEvents.size());
            LocalDateTime processedAt = LocalDateTime.now();
            tenantEvents.forEach(event -> event.markProcessed(processedAt));
            return;
        }

        Map<Long, List<BatchTransactionEvent>> eventsByUser = tenantEvents.stream()
                .collect(Collectors.groupingBy(BatchTransactionEvent::getUserId));

        eventsByUser.forEach((userId, userEvents) -> saveDailyReport(targetDate, windowStart, windowEnd, tenantId, userId, userEvents));

        LocalDateTime processedAt = LocalDateTime.now();
        tenantEvents.forEach(event -> event.markProcessed(processedAt));
        log.info("Processed {} batch events for tenant {} date {}", tenantEvents.size(), tenantId, targetDate);
    }

    private void saveDailyReport(LocalDate targetDate,
                                 LocalDateTime windowStart,
                                 LocalDateTime windowEnd,
                                 String tenantId,
                                 Long userId,
                                 List<BatchTransactionEvent> events) {
        double totalAmount = events.stream()
                .map(BatchTransactionEvent::getAmount)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();
        int count = events.size();
        double averageAmount = count > 0 ? totalAmount / count : 0.0;
        String dominantChannel = events.stream()
                .map(BatchTransactionEvent::getChannel)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(channel -> channel, Collectors.counting()))
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        DailyTransactionReport report = DailyTransactionReport.builder()
                .tenantId(tenantId)
                .reportDate(targetDate)
                .userId(userId)
                .transactionCount(count)
                .totalAmount(totalAmount)
                .averageAmount(averageAmount)
                .dominantChannel(dominantChannel)
                .windowStart(windowStart)
                .windowEnd(windowEnd)
                .createdAt(LocalDateTime.now())
                .build();

        dailyTransactionReportRepository.save(report);
    }
}
