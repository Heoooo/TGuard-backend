package com.tguard.tguard_backend.kafka.controller;

import com.tguard.tguard_backend.common.ApiResponse;
import com.tguard.tguard_backend.kafka.dto.DlqMessageResponse;
import com.tguard.tguard_backend.kafka.entity.DlqTransactionRetry;
import com.tguard.tguard_backend.kafka.repository.DlqTransactionRetryRepository;
import com.tguard.tguard_backend.kafka.service.DlqRetryService;
import com.tguard.tguard_backend.kafka.config.KafkaTopicProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/dlq")
@RequiredArgsConstructor
public class DlqAdminController {

    private final DlqTransactionRetryRepository dlqTransactionRetryRepository;
    private final DlqRetryService dlqRetryService;
    private final KafkaTopicProperties kafkaTopicProperties;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DlqMessageResponse>>> getDlqMessages() {
        List<DlqMessageResponse> messages = dlqTransactionRetryRepository.findTop100ByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    @PostMapping("/retry")
    public ResponseEntity<ApiResponse<String>> retryMessage(@Valid @RequestBody DlqRetryRequest request) {
        dlqRetryService.retrySingle(request.messageId());
        return ResponseEntity.ok(ApiResponse.success("retry-dispatched"));
    }

    private DlqMessageResponse toResponse(DlqTransactionRetry retry) {
        return new DlqMessageResponse(
                retry.getId(),
                dlqTopic(),
                retry.getTenantId(),
                retry.getTransactionId(),
                retry.getPayload(),
                retry.getErrorMessage(),
                retry.getErrorType(),
                retry.getAttemptCount(),
                retry.getCreatedAt(),
                retry.getLastFailedAt(),
                retry.getNextRetryAt()
        );
    }

    private String dlqTopic() {
        String topic = kafkaTopicProperties.dlq();
        if (topic == null || topic.isBlank()) {
            return "transactions-dlq";
        }
        return topic;
    }

    public record DlqRetryRequest(@NotNull Long messageId) {
    }
}
