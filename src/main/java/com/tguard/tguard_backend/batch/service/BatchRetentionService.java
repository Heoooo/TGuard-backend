package com.tguard.tguard_backend.batch.service;

import com.tguard.tguard_backend.batch.repository.BatchTransactionEventRepository;
import com.tguard.tguard_backend.common.pipeline.PipelineProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchRetentionService {

    private final BatchTransactionEventRepository batchTransactionEventRepository;
    private final PipelineProperties pipelineProperties;

    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void purgeProcessedEvents() {
        int retentionDays = Math.max(1, pipelineProperties.batch().retentionDays());
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        List<String> tenantIds = batchTransactionEventRepository.findDistinctTenantIds();

        for (String tenantId : tenantIds) {
            batchTransactionEventRepository.deleteByTenantIdAndProcessedTrueAndProcessedAtBefore(tenantId, cutoff);
        }

        log.debug("Batch retention cleanup executed with cutoff {}", cutoff);
    }
}
