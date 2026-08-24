package com.tguard.tguard_backend.kafka.repository;

import com.tguard.tguard_backend.kafka.entity.TransactionOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionOutboxEventRepository extends JpaRepository<TransactionOutboxEvent, Long> {

    List<TransactionOutboxEvent> findTop50ByPublishedFalseAndNextRetryAtBeforeOrderByCreatedAtAsc(LocalDateTime now);
}
