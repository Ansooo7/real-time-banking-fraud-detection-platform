package com.ukbank.fraudplatform.service;

import com.ukbank.fraudplatform.dto.AnalyticsSummaryResponse;
import com.ukbank.fraudplatform.model.AlertStatus;
import com.ukbank.fraudplatform.model.Transaction;
import com.ukbank.fraudplatform.model.TransactionStatus;
import com.ukbank.fraudplatform.repository.FraudAlertRepository;
import com.ukbank.fraudplatform.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final TransactionRepository transactionRepository;
    private final FraudAlertRepository fraudAlertRepository;

    public AnalyticsSummaryResponse getAnalyticsSummary() {
        ZonedDateTime cutoff = ZonedDateTime.now().minusHours(24);
        List<Transaction> recentTransactions = transactionRepository.findAll();

        long totalCount = recentTransactions.size();
        long approved = 0;
        long review = 0;
        long blocked = 0;
        BigDecimal totalVolume = BigDecimal.ZERO;
        BigDecimal totalBlockedAmount = BigDecimal.ZERO;

        Map<String, Long> channels = new HashMap<>();
        Map<String, Long> riskTiers = new HashMap<>();
        riskTiers.put("LOW_RISK_0_30", 0L);
        riskTiers.put("MEDIUM_RISK_31_70", 0L);
        riskTiers.put("HIGH_RISK_71_100", 0L);

        for (Transaction tx : recentTransactions) {
            totalVolume = totalVolume.add(tx.getAmount());
            String ch = tx.getChannel() != null ? tx.getChannel().name() : "OTHER";
            channels.put(ch, channels.getOrDefault(ch, 0L) + 1);

            int score = tx.getRiskScore() != null ? tx.getRiskScore() : 0;
            if (score <= 30) {
                riskTiers.put("LOW_RISK_0_30", riskTiers.get("LOW_RISK_0_30") + 1);
            } else if (score <= 70) {
                riskTiers.put("MEDIUM_RISK_31_70", riskTiers.get("MEDIUM_RISK_31_70") + 1);
            } else {
                riskTiers.put("HIGH_RISK_71_100", riskTiers.get("HIGH_RISK_71_100") + 1);
            }

            if (tx.getStatus() == TransactionStatus.APPROVED) {
                approved++;
            } else if (tx.getStatus() == TransactionStatus.REVIEW) {
                review++;
            } else if (tx.getStatus() == TransactionStatus.BLOCKED) {
                blocked++;
                totalBlockedAmount = totalBlockedAmount.add(tx.getAmount());
            }
        }

        double fraudRate = totalCount > 0 ? ((double) (review + blocked) / totalCount) * 100.0 : 0.0;
        long openAlerts = fraudAlertRepository.countByStatus(AlertStatus.PENDING_REVIEW);

        return AnalyticsSummaryResponse.builder()
                .totalTransactions24h(totalCount)
                .approvedCount(approved)
                .reviewCount(review)
                .blockedCount(blocked)
                .fraudRatePercent(Math.round(fraudRate * 100.0) / 100.0)
                .totalVolumeGbp(totalVolume)
                .totalBlockedAmountGbp(totalBlockedAmount)
                .openAlertsCount(openAlerts)
                .channelDistribution(channels)
                .riskTierDistribution(riskTiers)
                .build();
    }
}
