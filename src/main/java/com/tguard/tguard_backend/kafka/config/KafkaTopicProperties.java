package com.tguard.tguard_backend.kafka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "tguard.kafka.topics")
public class KafkaTopicProperties {

    private String realtime;
    private String batch;
    private String dlq;

    public String realtime() {
        return realtime;
    }

    public String batch() {
        return batch;
    }

    public String dlq() {
        return dlq;
    }

    public void setRealtime(String realtime) {
        this.realtime = realtime;
    }

    public void setBatch(String batch) {
        this.batch = batch;
    }

    public void setDlq(String dlq) {
        this.dlq = dlq;
    }
}
