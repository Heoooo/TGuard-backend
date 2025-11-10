package com.tguard.tguard_backend.common.pipeline;

import com.tguard.tguard_backend.batch.entity.BatchTransactionEvent;
import com.tguard.tguard_backend.batch.repository.BatchTransactionEventRepository;
import com.tguard.tguard_backend.common.notification.SlackNotifier;
import com.tguard.tguard_backend.kafka.repository.DlqTransactionRetryRepository;
import com.tguard.tguard_backend.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class PipelineMonitoringService {

    private final BatchTransactionEventRepository batchTransactionEventRepository;
    private final DlqTransactionRetryRepository dlqTransactionRetryRepository;
    private final TransactionRepository transactionRepository;
    private final SlackNotifier slackNotifier;
    private final PipelineProperties pipelineProperties;

    private final Map<String, Boolean> batchAlertState = new ConcurrentHashMap<>();
    private final Map<String, Boolean> dlqAlertState = new ConcurrentHashMap<>();

    @Scheduled(cron = "0 */5 * * * *")
    @Transactional(readOnly = true)
    public void monitorPipelines() {
        monitorBatchQueue();
        monitorDlqQueue();
        logRealtimeThroughput();
    }

    private void monitorBatchQueue() {
        int threshold = Math.max(1, pipelineProperties.monitoring().maxUnprocessedEvents());
        int stalenessMinutes = Math.max(5, pipelineProperties.monitoring().monitoringWindowMinutes());
        List<Object[]> pending = batchTransactionEventRepository.countPendingByTenant();
        Set<String> seenTenants = new HashSet<>();

        for (Object[] tuple : pending) {
            String tenantId = (String) tuple[0];
            seenTenants.add(tenantId);
            long count = ((Number) tuple[1]).longValue();
            boolean overThreshold = count > threshold;

            BatchTransactionEvent oldest = batchTransactionEventRepository
                    .findTop1ByTenantIdAndProcessedFalseOrderByTransactionTimeAsc(tenantId)
                    .orElse(null);

            boolean stale = false;
            if (oldest != null && oldest.getTransactionTime() != null) {
                long ageMinutes = Duration.between(oldest.getTransactionTime(), LocalDateTime.now()).toMinutes();
                stale = ageMinutes >= stalenessMinutes;
                if (stale) {
                    log.warn("Tenant {} batch queue is stale. ageMinutes={} pendingCount={} oldestId={}", tenantId, ageMinutes, count, oldest.getId());
                }
            }

            if (overThreshold || stale) {
                Boolean previous = batchAlertState.put(tenantId, true);
                if (!Boolean.TRUE.equals(previous)) {
                    sendBatchAlert(tenantId, count, oldest);
                }
            } else {
                Boolean previous = batchAlertState.put(tenantId, false);
                if (Boolean.TRUE.equals(previous)) {
                    sendBatchRecovery(tenantId);
                }
            }
        }

        batchAlertState.forEach((tenantId, state) -> {
            if (!seenTenants.contains(tenantId) && Boolean.TRUE.equals(state)) {
                batchAlertState.put(tenantId, false);
                sendBatchRecovery(tenantId);
            }
        });
    }

    private void monitorDlqQueue() {
        List<Object[]> pending = dlqTransactionRetryRepository.countPendingByTenant();
        Set<String> seenTenants = new HashSet<>();
        for (Object[] tuple : pending) {
            String tenantId = (String) tuple[0];
            seenTenants.add(tenantId);
            long count = ((Number) tuple[1]).longValue();
            if (count > 0) {
                Boolean previous = dlqAlertState.put(tenantId, true);
                if (!Boolean.TRUE.equals(previous)) {
                    slackNotifier.sendMessage(String.format(
                            ":warning: Tenant %s has %d DLQ retry items pending.",
                            tenantId, count
                    ));
                }
            } else {
                Boolean previous = dlqAlertState.put(tenantId, false);
                if (Boolean.TRUE.equals(previous)) {
                    slackNotifier.sendMessage(String.format(
                            ":white_check_mark: Tenant %s DLQ retry queue has recovered.", tenantId
                    ));
                }
            }
        }

        dlqAlertState.forEach((tenantId, state) -> {
            if (!seenTenants.contains(tenantId) && Boolean.TRUE.equals(state)) {
                dlqAlertState.put(tenantId, false);
                slackNotifier.sendMessage(String.format(
                        ":white_check_mark: Tenant %s DLQ retry queue has recovered.", tenantId
                ));
            }
        });
    }

    private void logRealtimeThroughput() {
        int windowMinutes = Math.max(5, pipelineProperties.monitoring().monitoringWindowMinutes());
        LocalDateTime since = LocalDateTime.now().minusMinutes(windowMinutes);
        List<Object[]> counts = transactionRepository.countTransactionsSince(since);
        for (Object[] tuple : counts) {
            String tenantId = (String) tuple[0];
            long count = ((Number) tuple[1]).longValue();
            log.info("Tenant {} processed {} transactions in the last {} minutes", tenantId, count, windowMinutes);
        }
    }

    private void sendBatchAlert(String tenantId, long count, BatchTransactionEvent oldest) {
        String oldestTime = "unknown";
        if (oldest != null && oldest.getTransactionTime() != null) {
            oldestTime = oldest.getTransactionTime().toString();
        }
        slackNotifier.sendMessage(String.format(
                ":warning: Tenant %s batch queue above threshold. pendingCount=%d oldestEvent=%s",
                tenantId, count, oldestTime
        ));
    }

    private void sendBatchRecovery(String tenantId) {
        slackNotifier.sendMessage(String.format(
                ":white_check_mark: Tenant %s batch queue back within limits.", tenantId
        ));
    }
}
