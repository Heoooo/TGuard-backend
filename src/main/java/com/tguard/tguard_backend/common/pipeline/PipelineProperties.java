package com.tguard.tguard_backend.common.pipeline;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tguard.pipeline")
public record PipelineProperties(
        DlqProperties dlq,
        BatchProperties batch,
        MonitoringProperties monitoring
) {

    public PipelineProperties {
        if (dlq == null) {
            dlq = new DlqProperties(3, 60, 2);
        }
        if (batch == null) {
            batch = new BatchProperties(7);
        }
        if (monitoring == null) {
            monitoring = new MonitoringProperties(500, 15);
        }
    }

    public record DlqProperties(
            int maxAttempts,
            int baseRetryDelaySeconds,
            int warnAttempts
    ) {
        public DlqProperties {
            maxAttempts = Math.max(1, maxAttempts);
            baseRetryDelaySeconds = Math.max(1, baseRetryDelaySeconds);
            if (warnAttempts <= 0) {
                warnAttempts = Math.max(1, maxAttempts - 1);
            } else {
                warnAttempts = Math.min(warnAttempts, maxAttempts);
            }
        }
    }

    public record BatchProperties(
            int retentionDays
    ) { }

    public record MonitoringProperties(
            int maxUnprocessedEvents,
            int monitoringWindowMinutes
    ) { }
}
