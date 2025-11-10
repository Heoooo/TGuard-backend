package com.tguard.tguard_backend.detection.service;

import com.tguard.tguard_backend.detection.entity.DetectionResult;
import com.tguard.tguard_backend.detection.repository.DetectionResultRepository;
import com.tguard.tguard_backend.notification.DefaultUserNotifier;
import com.tguard.tguard_backend.rule.entity.Rule;
import com.tguard.tguard_backend.rule.repository.RuleRepository;
import com.tguard.tguard_backend.transaction.entity.Transaction;
import com.tguard.tguard_backend.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DetectionResultService {

    private static final String GEO_LOCATION_RULE = "Geo-Location Change Rule";
    private static final String ABROAD_RULE = "Abroad Transaction Rule";
    private static final String NIGHT_RULE = "Night Time Transaction Rule";
    private static final String RAPID_RULE = "Rapid Sequential Transactions Rule";

    private static final int ALERT_THRESHOLD = 60;
    private static final double PROBABILITY_ALERT_THRESHOLD = 0.65;

    private static final Map<String, Integer> RULE_BASE_SCORES = Map.of(
            "High Amount Rule", 50,
            GEO_LOCATION_RULE, 30,
            ABROAD_RULE, 40,
            RAPID_RULE, 30,
            NIGHT_RULE, 15,
            "Device Change Rule", 20
    );

    private final RuleRepository ruleRepository;
    private final DetectionResultRepository detectionResultRepository;
    private final TransactionRepository transactionRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final DefaultUserNotifier userNotifier;

    @Transactional
    public void analyzeAndSave(String tenantId, Transaction transaction) {
        List<Rule> activeRules = ruleRepository.findByTenantIdAndActiveTrue(tenantId);

        if (activeRules.isEmpty()) {
            return;
        }

        RiskContext context = new RiskContext();
        AmountStats amountStats = calculateAmountStats(tenantId, transaction).orElse(null);
        initializeAmountStats(transaction, context, amountStats);

        List<RiskFactor> factors = new ArrayList<>();
        for (Rule rule : activeRules) {
            evaluateRule(rule, transaction, context, amountStats).ifPresent(factors::add);
        }

        if (factors.isEmpty()) {
            return;
        }

        int totalScore = factors.stream().mapToInt(RiskFactor::score).sum();
        double probability = computeProbability(context);
        context.fraudProbability = probability;

        if (totalScore < ALERT_THRESHOLD && probability < PROBABILITY_ALERT_THRESHOLD) {
            return;
        }

        RiskFactor representative = factors.stream()
                .max(Comparator.comparingInt(RiskFactor::score))
                .orElse(factors.get(0));

        String summary = buildSummary(totalScore, probability, factors);
        saveDetectionResult(tenantId, transaction, representative.rule(), totalScore, probability, summary);
        Transaction hydrated = transactionRepository.findWithUserByIdAndTenantId(transaction.getId(), tenantId)
                .orElse(transaction);
        userNotifier.notifyFraud(hydrated, summary);
    }

    private Optional<RiskFactor> evaluateRule(Rule rule, Transaction transaction, RiskContext context, AmountStats amountStats) {
        return switch (rule.getType()) {
            case AMOUNT -> evaluateAmountRule(rule, transaction, context, amountStats);
            case LOCATION -> evaluateLocationRule(rule, transaction, context);
            case DEVICE -> evaluateDeviceRule(rule, transaction, context);
            case PATTERN -> evaluatePatternRule(rule, transaction, context);
            default -> Optional.empty();
        };
    }

    private Optional<RiskFactor> evaluateAmountRule(Rule rule, Transaction transaction, RiskContext context, AmountStats amountStats) {
        Double amount = transaction.getAmount();
        if (amount == null) {
            return Optional.empty();
        }

        if (amountStats != null && amountStats.mean() > 0) {
            context.amountRatio = amount / amountStats.mean();
            if (amountStats.stdDev() > 0) {
                context.amountZScore = (amount - amountStats.mean()) / amountStats.stdDev();
            }
        }

        int baseScore = scoreFor(rule.getRuleName(), 40);
        int score = 0;
        List<String> reasons = new ArrayList<>();

        if (amount > 3_000_000d) {
            score += baseScore;
            reasons.add(String.format("Current amount %,.0f KRW (above 3,000,000 threshold)", amount));
        }

        if (context.amountZScore > 2.0) {
            score += Math.max(10, baseScore / 2);
            reasons.add(String.format("Significantly higher than recent average (z-score %.2f)", context.amountZScore));
        }

        if (score == 0) {
            return Optional.empty();
        }

        return Optional.of(new RiskFactor(rule, score, String.join("; ", reasons)));
    }

    private Optional<RiskFactor> evaluateLocationRule(Rule rule, Transaction transaction, RiskContext context) {
        if (ABROAD_RULE.equals(rule.getRuleName())) {
            boolean abroad = abroadTransactionCheck(transaction);
            context.abroad = context.abroad || abroad;
            if (abroad) {
                int score = scoreFor(rule.getRuleName(), 40);
                String description = String.format("Overseas transaction detected (location: %s)", defaultString(transaction.getLocation(), "unknown"));
                return Optional.of(new RiskFactor(rule, score, description));
            }
            return Optional.empty();
        }

        boolean changed = trackLocationChange(transaction);
        context.locationChanged = context.locationChanged || changed;
        if (!changed) {
            return Optional.empty();
        }
        int score = scoreFor(rule.getRuleName(), 30);
        String description = String.format("Location differs from recent history (%s)", defaultString(transaction.getLocation(), "unknown"));
        return Optional.of(new RiskFactor(rule, score, description));
    }

    private Optional<RiskFactor> evaluateDeviceRule(Rule rule, Transaction transaction, RiskContext context) {
        boolean changed = trackDeviceChange(transaction);
        context.deviceChanged = context.deviceChanged || changed;
        if (!changed) {
            return Optional.empty();
        }
        int score = scoreFor(rule.getRuleName(), 20);
        String description = String.format("Transaction from unfamiliar device (info: %s)", defaultString(transaction.getDeviceInfo(), "unknown"));
        return Optional.of(new RiskFactor(rule, score, description));
    }

    private Optional<RiskFactor> evaluatePatternRule(Rule rule, Transaction transaction, RiskContext context) {
        if (RAPID_RULE.equals(rule.getRuleName())) {
            long recentCount = trackRapidSequentialCount(transaction);
            context.rapidCount = Math.max(context.rapidCount, recentCount);
            if (recentCount >= 2) {
                int score = scoreFor(rule.getRuleName(), 30);
                String description = String.format("Multiple transactions within 1 minute (total %d)", recentCount + 1);
                return Optional.of(new RiskFactor(rule, score, description));
            }
            return Optional.empty();
        }
        if (NIGHT_RULE.equals(rule.getRuleName())) {
            LocalDateTime time = transaction.getTransactionTime();
            if (time != null) {
                int hour = time.getHour();
                if (hour >= 1 && hour <= 5) {
                    context.nightTime = true;
                    int score = scoreFor(rule.getRuleName(), 15);
                    String description = String.format("Late-night transaction (%02d h)", hour);
                    return Optional.of(new RiskFactor(rule, score, description));
                }
            }
        }
        return Optional.empty();
    }

    private void initializeAmountStats(Transaction transaction, RiskContext context, AmountStats amountStats) {
        Double amount = transaction.getAmount();
        if (amount == null) {
            context.amountRatio = 1.0;
            context.amountZScore = 0.0;
            return;
        }
        if (amountStats != null && amountStats.mean() > 0) {
            context.amountRatio = amount / amountStats.mean();
            if (amountStats.stdDev() > 0) {
                context.amountZScore = (amount - amountStats.mean()) / amountStats.stdDev();
            }
        } else {
            context.amountRatio = amount / 3_000_000d;
            context.amountZScore = 0.0;
        }
    }

    private Optional<AmountStats> calculateAmountStats(String tenantId, Transaction transaction) {
        List<Transaction> recent = transactionRepository.findTop10ByTenantIdAndUserOrderByTransactionTimeDesc(tenantId, transaction.getUser());
        List<Double> amounts = recent.stream()
                .filter(t -> !Objects.equals(t.getId(), transaction.getId()))
                .map(Transaction::getAmount)
                .filter(Objects::nonNull)
                .toList();
        if (amounts.isEmpty()) {
            return Optional.empty();
        }
        double mean = amounts.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = amounts.stream()
                .mapToDouble(value -> Math.pow(value - mean, 2))
                .average()
                .orElse(0.0);
        double stdDev = Math.sqrt(variance);
        return Optional.of(new AmountStats(mean, stdDev));
    }

    private boolean trackLocationChange(Transaction transaction) {
        if (!hasText(transaction.getLocation())) {
            return false;
        }
        String key = "tenant:" + transaction.getTenantId() + ":transaction:recent:location:" + transaction.getUser().getId();
        List<String> recentLocations = redisTemplate.opsForList().range(key, 0, -1);
        boolean changed = recentLocations != null && recentLocations.stream()
                .filter(this::hasText)
                .anyMatch(loc -> !loc.equals(transaction.getLocation()));

        redisTemplate.opsForList().rightPush(key, transaction.getLocation());
        redisTemplate.expire(key, Duration.ofMinutes(10));
        return changed;
    }

    private boolean trackDeviceChange(Transaction transaction) {
        if (!hasText(transaction.getDeviceInfo())) {
            return false;
        }
        String key = "tenant:" + transaction.getTenantId() + ":transaction:recent:device:" + transaction.getUser().getId();
        List<String> recentDevices = redisTemplate.opsForList().range(key, 0, -1);
        boolean changed = recentDevices != null && recentDevices.stream()
                .filter(this::hasText)
                .anyMatch(dev -> !dev.equals(transaction.getDeviceInfo()));

        redisTemplate.opsForList().rightPush(key, transaction.getDeviceInfo());
        redisTemplate.expire(key, Duration.ofMinutes(30));
        return changed;
    }

    private long trackRapidSequentialCount(Transaction transaction) {
        String key = "tenant:" + transaction.getTenantId() + ":transaction:recent:timestamps:" + transaction.getUser().getId();
        LocalDateTime occurred = transaction.getTransactionTime();
        if (occurred == null) {
            occurred = LocalDateTime.now();
        }
        final LocalDateTime occurredFinal = occurred;

        List<String> timestamps = redisTemplate.opsForList().range(key, 0, -1);
        long recentCount = 0;
        if (timestamps != null) {
            recentCount = timestamps.stream()
                    .map(LocalDateTime::parse)
                    .filter(ts -> ts.isAfter(occurredFinal.minusMinutes(1)))
                    .count();
        }

        redisTemplate.opsForList().rightPush(key, occurredFinal.toString());
        redisTemplate.expire(key, Duration.ofMinutes(1));
        return recentCount;
    }

    @Transactional
    protected void saveDetectionResult(String tenantId, Transaction transaction, Rule representativeRule, int totalScore, double probability, String reason) {
        DetectionResult result = DetectionResult.builder()
                .tenantId(tenantId)
                .transaction(transaction)
                .rule(representativeRule)
                .suspicious(true)
                .riskScore(totalScore)
                .probability(probability)
                .reason(reason)
                .build();

        detectionResultRepository.save(result);
    }

    private String buildSummary(int totalScore, double probability, List<RiskFactor> factors) {
        String detail = factors.stream()
                .map(f -> String.format("%s (+%d pts): %s", f.rule().getRuleName(), f.score(), f.description()))
                .collect(Collectors.joining(" | "));
        return String.format("Total score %d (fraud probability %.1f%%) - %s", totalScore, probability * 100, detail);
    }

    private int scoreFor(String ruleName, int defaultScore) {
        return RULE_BASE_SCORES.getOrDefault(ruleName, defaultScore);
    }

    private double computeProbability(RiskContext context) {
        double logRatio = Math.log(Math.max(context.amountRatio, 1.0));
        double positiveZ = Math.max(context.amountZScore, 0.0);
        double z = -3.0;
        z += 1.5 * logRatio;
        z += 0.9 * positiveZ;
        if (context.locationChanged) {
            z += 0.8;
        }
        if (context.deviceChanged) {
            z += 0.6;
        }
        z += 0.5 * Math.min(context.rapidCount, 3);
        if (context.nightTime) {
            z += 0.3;
        }
        if (context.abroad) {
            z += 1.0;
        }
        double probability = 1.0 / (1.0 + Math.exp(-z));
        if (Double.isFinite(probability)) {
            return probability;
        }
        return 0.5;
    }

    private String defaultString(String value, String fallback) {
        if (hasText(value)) {
            return value;
        }
        return fallback;
    }

    private boolean abroadTransactionCheck(Transaction transaction) {
        String location = transaction.getLocation();
        return hasText(location) && !"Korea".equalsIgnoreCase(location.trim());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record RiskFactor(Rule rule, int score, String description) { }

    private record AmountStats(double mean, double stdDev) { }

    private static class RiskContext {
        double amountRatio = 1.0;
        double amountZScore = 0.0;
        boolean locationChanged = false;
        boolean deviceChanged = false;
        long rapidCount = 0;
        boolean nightTime = false;
        boolean abroad = false;
        double fraudProbability = 0.0;
    }
}
