package com.tguard.tguard_backend.detection.service;

import com.tguard.tguard_backend.detection.entity.DetectionResult;
import com.tguard.tguard_backend.detection.repository.DetectionResultRepository;
import com.tguard.tguard_backend.notification.DefaultUserNotifier;
import com.tguard.tguard_backend.rule.entity.Rule;
import com.tguard.tguard_backend.rule.repository.RuleRepository;
import com.tguard.tguard_backend.rule.service.RuleService;
import com.tguard.tguard_backend.transaction.entity.Transaction;
import com.tguard.tguard_backend.transaction.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DetectionResultService {

    private final RuleRepository ruleRepository;
    private final TransactionRepository transactionRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final DefaultUserNotifier userNotifier;

    /**
     * 거래에 대해 이상 여부를 분석하고 결과를 저장
     */
    public void analyzeAndSave(Transaction transaction) {
        List<Rule> activeRules = ruleRepository.findAll()
                .stream()
                .filter(Rule::isActive)
                .toList();

        for (Rule rule : activeRules) {
            boolean suspicious = switch (rule.getRuleName()) {
                case "High Amount Rule" -> highAmountCheck(transaction);
                case "Geo-Location Change Rule" -> geoLocationChangeCheck(transaction);
                case "Abroad Transaction Rule" -> abroadTransactionCheck(transaction);
                case "Night Time Transaction Rule" -> nightTimeTransactionCheck(transaction);
                case "Device Change Rule" -> deviceChangeCheck(transaction);
                case "Rapid Sequential Transactions Rule" -> rapidSequentialTransactionsCheck(transaction);
                default -> false;
            };

            if (suspicious) {
                saveDetectionResult(transaction, rule);
                userNotifier.notifyFraud(transaction, rule.getRuleName()); // SMS+Web 알림
                break; // 한 Rule 탐지 시 종료 (중복 탐지 방지)
            }
        }
    }

    /**
     * 탐지 알고리즘: 고액 거래 여부
     */
    private boolean highAmountCheck(Transaction t) {
        return t.getAmount() != null && t.getAmount() > 3_000_000;
    }

    /** 2. 10분 내 위치 변경 탐지 (Redis) */
    private boolean geoLocationChangeCheck(Transaction transaction) {
        String key = "transaction:recent:location:" + transaction.getUser().getId();

        List<String> recentLocations = redisTemplate.opsForList().range(key, 0, -1);
        boolean locationChanged = recentLocations != null &&
                recentLocations.stream().anyMatch(loc -> !loc.equals(transaction.getLocation()));

        redisTemplate.opsForList().rightPush(key, transaction.getLocation());
        redisTemplate.expire(key, Duration.ofMinutes(10));

        return locationChanged;
    }

    /** 3. 해외 거래 탐지 */
    private boolean abroadTransactionCheck(Transaction transaction) {
        return !"Korea".equalsIgnoreCase(transaction.getLocation());
    }

    /** 4. 새벽 시간 거래 탐지 */
    private boolean nightTimeTransactionCheck(Transaction transaction) {
        int hour = transaction.getTransactionTime().getHour();
        return (hour >= 1 && hour <= 5);
    }

    /** 탐지 결과 저장 로직 (추가 구현 예정) */
    private void saveDetectionResult(Transaction transaction, Rule rule) {
        // DetectionResult 엔티티에 기록 (탐지 시각, Rule 이름, 거래 ID 등)
        // 예: detectionResultRepository.save(new DetectionResult(...));
    }

    private boolean deviceChangeCheck(Transaction transaction) {
        String key = "transaction:recent:device:" + transaction.getUser().getId();

        List<String> recentDevices = redisTemplate.opsForList().range(key, 0, -1);
        boolean deviceChanged = recentDevices != null &&
                recentDevices.stream().anyMatch(dev -> !dev.equals(transaction.getDeviceInfo()));

        redisTemplate.opsForList().rightPush(key, transaction.getDeviceInfo());
        redisTemplate.expire(key, Duration.ofMinutes(30));

        return deviceChanged;
    }

    private boolean rapidSequentialTransactionsCheck(Transaction transaction) {
        String key = "transaction:recent:timestamps:" + transaction.getUser().getId();
        LocalDateTime now = LocalDateTime.now();

        List<String> timestamps = redisTemplate.opsForList().range(key, 0, -1);

        long recentCount = timestamps != null
                ? timestamps.stream()
                .map(LocalDateTime::parse)
                .filter(ts -> ts.isAfter(now.minusMinutes(1)))
                .count()
                : 0;

        redisTemplate.opsForList().rightPush(key, now.toString());
        redisTemplate.expire(key, Duration.ofMinutes(1));

        return recentCount >= 2;
    }
}