package com.ukbank.fraudplatform.service;

import com.ukbank.fraudplatform.model.Merchant;
import com.ukbank.fraudplatform.model.RiskProfile;
import com.ukbank.fraudplatform.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

class RuleEngineServiceTest {

    private RuleEngineService ruleEngineService;

    @BeforeEach
    void setUp() {
        ruleEngineService = new RuleEngineService();
    }

    @Test
    @DisplayName("Normal transaction should have low rule score and no critical triggers")
    void testNormalTransactionEvaluation() {
        Transaction tx = Transaction.builder()
                .amount(BigDecimal.valueOf(25.50))
                .latitude(51.5074)
                .longitude(-0.1278)
                .createdAt(ZonedDateTime.of(2026, 8, 25, 14, 0, 0, 0, ZoneId.of("UTC")))
                .build();

        RiskProfile profile = RiskProfile.builder()
                .avgTransactionAmount30d(BigDecimal.valueOf(30.00))
                .txCountLast24h(2)
                .lastKnownLatitude(51.5074)
                .lastKnownLongitude(-0.1278)
                .build();

        Merchant merchant = Merchant.builder()
                .mcc("5411") // Groceries
                .riskScoreBase(5)
                .build();

        RuleEngineService.RuleEvaluationResult result = ruleEngineService.evaluate(tx, profile, merchant, false);

        assertNotNull(result);
        assertEquals(0, result.getRuleScore());
        assertTrue(result.getTriggeredRules().isEmpty());
        assertFalse(result.isCriticalOverride());
    }

    @Test
    @DisplayName("Transaction exceeding £5000 should trigger RULE_AMOUNT_EXCEEDS_5K")
    void testLargeAmountTrigger() {
        Transaction tx = Transaction.builder()
                .amount(BigDecimal.valueOf(7500.00))
                .latitude(51.5074)
                .longitude(-0.1278)
                .createdAt(ZonedDateTime.of(2026, 8, 25, 14, 0, 0, 0, ZoneId.of("UTC")))
                .build();

        RiskProfile profile = RiskProfile.builder()
                .avgTransactionAmount30d(BigDecimal.valueOf(50.00))
                .txCountLast24h(1)
                .lastKnownLatitude(51.5074)
                .lastKnownLongitude(-0.1278)
                .build();

        RuleEngineService.RuleEvaluationResult result = ruleEngineService.evaluate(tx, profile, null, false);

        assertTrue(result.getRuleScore() >= 35);
        assertTrue(result.getTriggeredRules().contains("RULE_AMOUNT_EXCEEDS_5K"));
    }

    @Test
    @DisplayName("Night-time transaction with new device and Crypto MCC should accumulate high score")
    void testMultiVectorFraudTriggers() {
        Transaction tx = Transaction.builder()
                .amount(BigDecimal.valueOf(1200.00))
                .latitude(51.5074)
                .longitude(-0.1278)
                .createdAt(ZonedDateTime.of(2026, 8, 25, 3, 30, 0, 0, ZoneId.of("UTC"))) // 03:30 AM
                .build();

        RiskProfile profile = RiskProfile.builder()
                .avgTransactionAmount30d(BigDecimal.valueOf(100.00))
                .txCountLast24h(5)
                .lastKnownLatitude(51.5074)
                .lastKnownLongitude(-0.1278)
                .build();

        Merchant merchant = Merchant.builder()
                .mcc("6051") // Crypto
                .riskScoreBase(75)
                .build();

        RuleEngineService.RuleEvaluationResult result = ruleEngineService.evaluate(tx, profile, merchant, true);

        assertTrue(result.getRuleScore() >= 65);
        assertTrue(result.getTriggeredRules().contains("RULE_UNUSUAL_NIGHT_TIME_HOURS"));
        assertTrue(result.getTriggeredRules().contains("RULE_NEW_UNTRUSTED_DEVICE"));
        assertTrue(result.getTriggeredRules().contains("RULE_HIGH_RISK_MCC_CRYPTO"));
    }

    @Test
    @DisplayName("Impossible travel geo anomaly should trigger geographic rule")
    void testImpossibleTravelTrigger() {
        // Customer was in London (51.5074, -0.1278), transaction originates in Tokyo (35.6762, 139.6503)
        Transaction tx = Transaction.builder()
                .amount(BigDecimal.valueOf(150.00))
                .latitude(35.6762)
                .longitude(139.6503)
                .createdAt(ZonedDateTime.now())
                .build();

        RiskProfile profile = RiskProfile.builder()
                .avgTransactionAmount30d(BigDecimal.valueOf(150.00))
                .txCountLast24h(1)
                .lastKnownLatitude(51.5074)
                .lastKnownLongitude(-0.1278)
                .build();

        RuleEngineService.RuleEvaluationResult result = ruleEngineService.evaluate(tx, profile, null, false);

        assertTrue(result.getTriggeredRules().contains("RULE_GEOGRAPHIC_IMPOSSIBLE_TRAVEL"));
        assertTrue(result.getRuleScore() >= 35);
    }
}
