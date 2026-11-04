package com.finwise.service.impl;

import com.finwise.dto.recurring.RecurringTransactionRequest;
import com.finwise.dto.recurring.RecurringTransactionResponse;
import com.finwise.dto.subscription.ConfirmSubscriptionRequest;
import com.finwise.dto.subscription.DetectedSubscriptionResponse;
import com.finwise.dto.subscription.SubscriptionDetectionSummaryResponse;
import com.finwise.entity.RecurringFrequency;
import com.finwise.entity.Transaction;
import com.finwise.entity.TransactionType;
import com.finwise.repository.RecurringTransactionRepository;
import com.finwise.repository.TransactionRepository;
import com.finwise.service.RecurringTransactionService;
import com.finwise.service.SubscriptionDetectorService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionDetectorServiceImpl implements SubscriptionDetectorService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionDetectorServiceImpl.class);

    private final TransactionRepository transactionRepository;
    private final RecurringTransactionRepository recurringRepository;
    private final RecurringTransactionService recurringTransactionService;

    // Track user dismissed suggestions in-memory (per user session/tenant)
    private final Map<Long, Set<String>> dismissedCandidateIds = new ConcurrentHashMap<>();

    @Override
    @Transactional(readOnly = true)
    public SubscriptionDetectionSummaryResponse detectSubscriptions(Long userId, int lookbackMonths) {
        int windowMonths = lookbackMonths > 0 ? Math.min(lookbackMonths, 24) : 6;
        Instant since = Instant.now().minus(windowMonths * 30L, ChronoUnit.DAYS);

        List<Transaction> transactions = transactionRepository.findExpensesByUserIdSince(userId, since);
        log.debug("Found {} expense transactions for user id {} over the past {} months for subscription detection",
                transactions.size(), userId, windowMonths);

        // Fetch user's existing tracked recurring transaction titles to avoid recommending what's already tracked
        Set<String> existingRecurringTitles = recurringRepository.findByUserIdOrderByNextExecutionDateAsc(userId)
                .stream()
                .map(r -> r.getTitle().toLowerCase(Locale.ROOT).trim())
                .collect(Collectors.toSet());

        Set<String> dismissed = dismissedCandidateIds.getOrDefault(userId, Collections.emptySet());

        // Group transactions by normalized merchant / description
        Map<String, List<Transaction>> clustered = new HashMap<>();
        for (Transaction tx : transactions) {
            if (tx.getRecurringTransaction() != null) {
                continue; // Already associated with recurring subscription
            }
            String key = extractNormalizedKey(tx);
            if (!key.isBlank()) {
                clustered.computeIfAbsent(key, k -> new ArrayList<>()).add(tx);
            }
        }

        List<DetectedSubscriptionResponse> detectedList = new ArrayList<>();
        BigDecimal totalMonthly = BigDecimal.ZERO;
        BigDecimal totalAnnual = BigDecimal.ZERO;

        for (Map.Entry<String, List<Transaction>> entry : clustered.entrySet()) {
            String key = entry.getKey();
            List<Transaction> cluster = entry.getValue();

            // Needs at least 2 occurrences to establish a recurring pattern
            if (cluster.size() < 2) {
                continue;
            }

            cluster.sort(Comparator.comparing(Transaction::getTransactionDate));

            // 1. Analyze Amounts
            BigDecimal totalAmount = BigDecimal.ZERO;
            for (Transaction t : cluster) {
                totalAmount = totalAmount.add(t.getAmount());
            }
            BigDecimal avgAmount = totalAmount.divide(BigDecimal.valueOf(cluster.size()), 2, RoundingMode.HALF_EVEN);

            // Verify amount consistency: reject if amounts fluctuate wildly (> 20% variance)
            boolean amountConsistent = true;
            for (Transaction t : cluster) {
                BigDecimal diff = t.getAmount().subtract(avgAmount).abs();
                if (diff.compareTo(new BigDecimal("100.00")) > 0) {
                    double deviation = diff.divide(avgAmount, 4, RoundingMode.HALF_EVEN).doubleValue();
                    if (deviation > 0.20) {
                        amountConsistent = false;
                        break;
                    }
                }
            }
            if (!amountConsistent) {
                continue;
            }

            // 2. Analyze Intervals & Recurring Dates
            List<Long> intervals = new ArrayList<>();
            for (int i = 0; i < cluster.size() - 1; i++) {
                LocalDate d1 = LocalDate.ofInstant(cluster.get(i).getTransactionDate(), ZoneOffset.UTC);
                LocalDate d2 = LocalDate.ofInstant(cluster.get(i + 1).getTransactionDate(), ZoneOffset.UTC);
                long days = ChronoUnit.DAYS.between(d1, d2);
                if (days > 0) {
                    intervals.add(days);
                }
            }

            if (intervals.isEmpty()) {
                continue;
            }

            double avgInterval = intervals.stream().mapToLong(Long::longValue).average().orElse(0.0);
            RecurringFrequency frequency = determineFrequency(avgInterval);
            if (frequency == null) {
                continue; // Not a recognized recurring interval
            }

            // 3. Compute Estimated Annual Cost
            int annualMultiplier = getAnnualMultiplier(frequency);
            BigDecimal annualCost = avgAmount.multiply(BigDecimal.valueOf(annualMultiplier)).setScale(2, RoundingMode.HALF_EVEN);
            BigDecimal monthlyCost = frequency == RecurringFrequency.MONTHLY
                    ? avgAmount
                    : annualCost.divide(new BigDecimal("12"), 2, RoundingMode.HALF_EVEN);

            // 4. Determine Name & Metadata
            Transaction lastTx = cluster.get(cluster.size() - 1);
            LocalDate lastDate = LocalDate.ofInstant(lastTx.getTransactionDate(), ZoneOffset.UTC);
            LocalDate nextSuggested = calculateNextSuggestedDate(lastDate, frequency);

            String displayName = deriveDisplayName(cluster, key);
            String candidateId = "SUB-" + Math.abs(key.hashCode()) + "-" + avgAmount.intValue();

            // Skip if already confirmed or dismissed
            if (existingRecurringTitles.contains(displayName.toLowerCase(Locale.ROOT).trim()) || dismissed.contains(candidateId)) {
                continue;
            }

            double confidence = calculateConfidence(cluster.size(), intervals, avgInterval);

            List<String> descriptions = cluster.stream()
                    .map(Transaction::getDescription)
                    .filter(Objects::nonNull)
                    .distinct()
                    .limit(3)
                    .collect(Collectors.toList());

            DetectedSubscriptionResponse response = DetectedSubscriptionResponse.builder()
                    .candidateId(candidateId)
                    .subscriptionName(displayName)
                    .averagePayment(avgAmount)
                    .frequency(frequency)
                    .frequencyLabel(formatFrequencyLabel(frequency))
                    .estimatedAnnualCost(annualCost)
                    .lastPaymentDate(lastDate)
                    .suggestedNextPaymentDate(nextSuggested)
                    .status("SUGGESTION")
                    .isSuggestion(true)
                    .confidenceScore(confidence)
                    .occurrenceCount(cluster.size())
                    .accountId(lastTx.getAccount() != null ? lastTx.getAccount().getId() : null)
                    .accountName(lastTx.getAccount() != null ? lastTx.getAccount().getAccountName() : null)
                    .categoryId(lastTx.getCategory() != null ? lastTx.getCategory().getId() : null)
                    .categoryName(lastTx.getCategory() != null ? lastTx.getCategory().getName() : null)
                    .transactionDescriptions(descriptions)
                    .build();

            detectedList.add(response);
            totalMonthly = totalMonthly.add(monthlyCost);
            totalAnnual = totalAnnual.add(annualCost);
        }

        detectedList.sort(Comparator.comparing(DetectedSubscriptionResponse::getEstimatedAnnualCost).reversed());

        return SubscriptionDetectionSummaryResponse.builder()
                .detectedSubscriptionsCount(detectedList.size())
                .totalEstimatedMonthlyOutflow(totalMonthly)
                .totalEstimatedAnnualOutflow(totalAnnual)
                .notice("All detected recurring expenses are marked as suggestions until confirmed by you.")
                .subscriptions(detectedList)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DetectedSubscriptionResponse> getDetectedSubscriptions(Long userId) {
        return detectSubscriptions(userId, 6).getSubscriptions();
    }

    @Override
    @Transactional
    public RecurringTransactionResponse confirmDetectedSubscription(Long userId, ConfirmSubscriptionRequest request) {
        log.info("User {} confirmed detected subscription '{}' of ₹{}",
                userId, request.getSubscriptionName(), request.getAmount());

        LocalDate startDate = request.getStartDate() != null ? request.getStartDate() : LocalDate.now();

        RecurringTransactionRequest recurringRequest = RecurringTransactionRequest.builder()
                .title(request.getSubscriptionName())
                .accountId(request.getAccountId())
                .categoryId(request.getCategoryId())
                .amount(request.getAmount())
                .transactionType(TransactionType.EXPENSE)
                .frequency(request.getFrequency())
                .startDate(startDate)
                .nextExecutionDate(startDate)
                .autoPost(request.getAutoPost() != null ? request.getAutoPost() : true)
                .description("Confirmed subscription: " + request.getSubscriptionName())
                .build();

        return recurringTransactionService.createRecurringTransaction(recurringRequest, userId);
    }

    @Override
    public void dismissDetectedSubscription(Long userId, String candidateId) {
        if (candidateId != null && !candidateId.isBlank()) {
            dismissedCandidateIds.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(candidateId);
            log.info("User {} dismissed subscription suggestion {}", userId, candidateId);
        }
    }

    // --- Helper Heuristics ---

    private String extractNormalizedKey(Transaction tx) {
        if (tx.getMerchantName() != null && !tx.getMerchantName().isBlank()) {
            return cleanToken(tx.getMerchantName());
        }
        if (tx.getDescription() != null && !tx.getDescription().isBlank()) {
            return cleanToken(tx.getDescription());
        }
        return "";
    }

    private String cleanToken(String raw) {
        String clean = raw.toLowerCase(Locale.ROOT)
                .replaceAll("(?i)(pvt|ltd|limited|inc|corp|india|mumbai|delhi|bangalore|payment|bill|recharge|sub|subscription|monthly|autopay|upi|ref|txn|in|com)", " ")
                .replaceAll("[^a-z0-9 ]", " ")
                .trim()
                .replaceAll("\\s+", " ");
        
        // Take the primary root words (first 2 words if multi-word)
        String[] parts = clean.split(" ");
        if (parts.length > 2) {
            return parts[0] + " " + parts[1];
        }
        return clean;
    }

    private RecurringFrequency determineFrequency(double avgInterval) {
        if (avgInterval >= 5.0 && avgInterval <= 9.0) {
            return RecurringFrequency.WEEKLY;
        } else if (avgInterval >= 12.0 && avgInterval <= 18.0) {
            return RecurringFrequency.BI_WEEKLY;
        } else if (avgInterval >= 25.0 && avgInterval <= 35.0) {
            return RecurringFrequency.MONTHLY;
        } else if (avgInterval >= 80.0 && avgInterval <= 100.0) {
            return RecurringFrequency.QUARTERLY;
        } else if (avgInterval >= 340.0 && avgInterval <= 390.0) {
            return RecurringFrequency.YEARLY;
        }
        // Default to monthly if interval falls close to month
        if (avgInterval >= 20.0 && avgInterval <= 45.0) {
            return RecurringFrequency.MONTHLY;
        }
        return null;
    }

    private int getAnnualMultiplier(RecurringFrequency freq) {
        return switch (freq) {
            case DAILY -> 365;
            case WEEKLY -> 52;
            case BI_WEEKLY -> 26;
            case MONTHLY -> 12;
            case QUARTERLY -> 4;
            case YEARLY -> 1;
        };
    }

    private String formatFrequencyLabel(RecurringFrequency freq) {
        return switch (freq) {
            case DAILY -> "Daily";
            case WEEKLY -> "Weekly";
            case BI_WEEKLY -> "Bi-Weekly";
            case MONTHLY -> "Monthly";
            case QUARTERLY -> "Quarterly";
            case YEARLY -> "Yearly";
        };
    }

    private LocalDate calculateNextSuggestedDate(LocalDate lastDate, RecurringFrequency frequency) {
        LocalDate next = switch (frequency) {
            case DAILY -> lastDate.plusDays(1);
            case WEEKLY -> lastDate.plusWeeks(1);
            case BI_WEEKLY -> lastDate.plusWeeks(2);
            case MONTHLY -> lastDate.plusMonths(1);
            case QUARTERLY -> lastDate.plusMonths(3);
            case YEARLY -> lastDate.plusYears(1);
        };
        LocalDate today = LocalDate.now();
        while (next.isBefore(today)) {
            next = switch (frequency) {
                case DAILY -> next.plusDays(1);
                case WEEKLY -> next.plusWeeks(1);
                case BI_WEEKLY -> next.plusWeeks(2);
                case MONTHLY -> next.plusMonths(1);
                case QUARTERLY -> next.plusMonths(3);
                case YEARLY -> next.plusYears(1);
            };
        }
        return next;
    }

    private String deriveDisplayName(List<Transaction> cluster, String key) {
        for (Transaction t : cluster) {
            if (t.getMerchantName() != null && !t.getMerchantName().isBlank()) {
                return t.getMerchantName().trim();
            }
        }
        for (Transaction t : cluster) {
            if (t.getDescription() != null && !t.getDescription().isBlank()) {
                return t.getDescription().trim();
            }
        }
        return capitalize(key);
    }

    private String capitalize(String str) {
        if (str == null || str.isBlank()) return "";
        return Arrays.stream(str.split(" "))
                .filter(s -> !s.isBlank())
                .map(s -> Character.toUpperCase(s.charAt(0)) + s.substring(1))
                .collect(Collectors.joining(" "));
    }

    private double calculateConfidence(int count, List<Long> intervals, double avgInterval) {
        double base = 0.75;
        if (count >= 3) base += 0.10;
        if (count >= 5) base += 0.05;

        // Check variance of intervals
        double varSum = 0;
        for (Long interval : intervals) {
            double diff = interval - avgInterval;
            varSum += diff * diff;
        }
        double stdDev = Math.sqrt(varSum / intervals.size());
        if (stdDev <= 3.0) {
            base += 0.05;
        }
        return Math.min(0.95, base);
    }
}
