package com.ukbank.fraudplatform.service;

import com.ukbank.fraudplatform.model.Merchant;
import com.ukbank.fraudplatform.model.RiskProfile;
import com.ukbank.fraudplatform.model.Transaction;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class RuleEngineService {

    private static final BigDecimal LARGE_AMOUNT_THRESHOLD = new BigDecimal("5000.00");
    private static final double MAX_REASONABLE_KM_PER_HOUR = 800.0; // Commercial aircraft speed limit

    @Data
    @Builder
    public static class RuleEvaluationResult {
        private int ruleScore; // 0 to 100
        private List<String> triggeredRules;
        private Map<String, Object> ruleDetails;
        private boolean criticalOverride;
    }

    public RuleEvaluationResult evaluate(Transaction tx, RiskProfile profile, Merchant merchant, boolean isNewDevice) {
        List<String> triggers = new ArrayList<>();
        Map<String, Object> details = new HashMap<>();
        int score = 0;
        boolean critical = false;

        BigDecimal amount = tx.getAmount();
        BigDecimal avg30d = (profile != null && profile.getAvgTransactionAmount30d() != null)
                ? profile.getAvgTransactionAmount30d() : BigDecimal.valueOf(50.0);

        // 1. Unusually Large Transaction Amount Rule
        if (amount.compareTo(LARGE_AMOUNT_THRESHOLD) >= 0) {
            score += 35;
            triggers.add("RULE_AMOUNT_EXCEEDS_5K");
            details.put("large_amount", amount);
        } else if (avg30d.compareTo(BigDecimal.ZERO) > 0 && 
                   amount.compareTo(avg30d.multiply(BigDecimal.valueOf(4.0))) >= 0) {
            score += 25;
            triggers.add("RULE_AMOUNT_SPIKE_4X_HISTORICAL");
            details.put("amount_to_avg_ratio", amount.doubleValue() / avg30d.doubleValue());
        }

        // 2. Transaction Velocity Anomaly Rule
        int count24h = (profile != null && profile.getTxCountLast24h() != null) ? profile.getTxCountLast24h() : 0;
        if (count24h >= 8) {
            score += 30;
            triggers.add("RULE_HIGH_VELOCITY_24H_BURST");
            details.put("velocity_24h", count24h);
        } else if (count24h >= 4) {
            score += 15;
            triggers.add("RULE_ELEVATED_VELOCITY_24H");
            details.put("velocity_24h", count24h);
        }

        // 3. Geographic Anomaly / Impossible Travel Speed Rule
        if (profile != null && profile.getLastKnownLatitude() != null && tx.getLatitude() != null) {
            double distanceKm = calculateHaversineDistance(
                    profile.getLastKnownLatitude(), profile.getLastKnownLongitude(),
                    tx.getLatitude(), tx.getLongitude()
            );
            details.put("geo_distance_km", distanceKm);

            if (distanceKm > 500.0) {
                score += 35;
                triggers.add("RULE_GEOGRAPHIC_IMPOSSIBLE_TRAVEL");
                details.put("anomaly_distance_km", distanceKm);
            } else if (distanceKm > 150.0) {
                score += 15;
                triggers.add("RULE_GEOGRAPHIC_DEVIATION");
            }
        }

        // 4. Unusual Transaction Time (UK Night-time 01:00 - 05:00)
        ZonedDateTime txTime = tx.getCreatedAt() != null ? tx.getCreatedAt() : ZonedDateTime.now();
        int hour = txTime.getHour();
        if (hour >= 1 && hour <= 5) {
            score += 15;
            triggers.add("RULE_UNUSUAL_NIGHT_TIME_HOURS");
            details.put("tx_hour", hour);
        }

        // 5. New / Untrusted Device Rule
        if (isNewDevice) {
            score += 20;
            triggers.add("RULE_NEW_UNTRUSTED_DEVICE");
            details.put("is_new_device", true);
        }

        // 6. Suspicious Merchant Category Code (MCC)
        if (merchant != null) {
            String mcc = merchant.getMcc();
            if ("6051".equals(mcc)) { // Crypto / Quasi-Cash
                score += 30;
                triggers.add("RULE_HIGH_RISK_MCC_CRYPTO");
                details.put("merchant_mcc", mcc);
            } else if ("7995".equals(mcc)) { // Gambling / Betting
                score += 25;
                triggers.add("RULE_HIGH_RISK_MCC_GAMBLING");
                details.put("merchant_mcc", mcc);
            } else if ("4829".equals(mcc)) { // Wire Transfer / Money Order
                score += 25;
                triggers.add("RULE_HIGH_RISK_MCC_WIRE_TRANSFER");
                details.put("merchant_mcc", mcc);
            } else if (merchant.getRiskScoreBase() != null && merchant.getRiskScoreBase() >= 50) {
                score += 15;
                triggers.add("RULE_ELEVATED_RISK_MERCHANT");
            }
        }

        // Cap rule score between 0 and 100
        int finalScore = Math.min(100, Math.max(0, score));

        log.debug("Rule engine evaluated {} triggers, calculated score: {}", triggers.size(), finalScore);

        return RuleEvaluationResult.builder()
                .ruleScore(finalScore)
                .triggeredRules(triggers)
                .ruleDetails(details)
                .criticalOverride(critical)
                .build();
    }

    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round(R * c * 100.0) / 100.0;
    }
}
