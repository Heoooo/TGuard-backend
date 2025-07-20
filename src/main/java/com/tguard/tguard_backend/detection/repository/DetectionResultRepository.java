package com.tguard.tguard_backend.detection.repository;

import com.tguard.tguard_backend.detection.entity.DetectionResult;
import com.tguard.tguard_backend.transaction.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DetectionResultRepository extends JpaRepository<DetectionResult, Long> {
    Optional<DetectionResult> findByTransaction(Transaction transaction);
}
