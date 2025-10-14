package com.tguard.tguard_backend.common.pipeline;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tguard.pipeline")
public record PipelineProperties(
        DlqProperties dlq,
        BatchProperties batch,
        MonitoringProperties monitoring
) {

    public PipelineProperties {
        dlq = dlq != null ? dlq : new DlqProperties(3, 60, 2);
        batch = batch != null ? batch : new BatchProperties(7);
        monitoring = monitoring != null ? monitoring : new MonitoringProperties(500, 15);
    }

    public record DlqProperties(
            int maxAttempts,
            int baseRetryDelaySeconds,
            int warnAttempts
    ) {
        public DlqProperties {
            maxAttempts = Math.max(1, maxAttempts);
            baseRetryDelaySeconds = Math.max(1, baseRetryDelaySeconds);
            warnAttempts = warnAttempts <= 0 ? Math.max(1, maxAttempts - 1) : Math.min(warnAttempts, maxAttempts);
        }
    }

    public record BatchProperties(
            int retentionDays
    ) { }

    /**
     * monitoringWindowMinutes: how far back (minutes) we consider when checking lag/volume
     * maxUnprocessedEvents: threshold for batch queue size per tenant.
     */
    public record MonitoringProperties(
            int maxUnprocessedEvents,
            int monitoringWindowMinutes
    ) { }
}
