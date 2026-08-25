package com.ukbank.fraudplatform.service;

import com.ukbank.fraudplatform.model.TransactionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompositeRiskEvaluatorTest {

    private CompositeRiskEvaluator compositeRiskEvaluator;

    @BeforeEach
    void setUp() {
        compositeRiskEvaluator = new CompositeRiskEvaluator();
    }

    @Test
    @DisplayName("Scores <= 30 should result in APPROVED decision")
    void testApprovedDecision() {
        CompositeRiskEvaluator.CompositeEvaluation eval = compositeRiskEvaluator.evaluate(
                10, 20, false, List.of()
        );

        // 10 * 0.45 + 20 * 0.55 = 4.5 + 11.0 = 15.5 -> 16
        assertEquals(16, eval.getCompositeRiskScore());
        assertEquals(TransactionStatus.APPROVED, eval.getDecision());
        assertTrue(eval.getDecisionReason().toLowerCase().contains("approved"));
    }

    @Test
    @DisplayName("Scores between 31 and 70 should result in REVIEW decision")
    void testReviewDecision() {
        CompositeRiskEvaluator.CompositeEvaluation eval = compositeRiskEvaluator.evaluate(
                60, 50, false, List.of("RULE_HIGH_RISK_MCC_CRYPTO")
        );

        // 60 * 0.45 + 50 * 0.55 = 27 + 27.5 = 54.5 -> 55
        assertEquals(55, eval.getCompositeRiskScore());
        assertEquals(TransactionStatus.REVIEW, eval.getDecision());
        assertTrue(eval.getDecisionReason().toLowerCase().contains("review"));
        assertTrue(eval.getDecisionReason().contains("RULE_HIGH_RISK_MCC_CRYPTO"));
    }

    @Test
    @DisplayName("Scores > 70 should result in BLOCKED decision")
    void testBlockedDecision() {
        CompositeRiskEvaluator.CompositeEvaluation eval = compositeRiskEvaluator.evaluate(
                85, 90, false, List.of("RULE_GEOGRAPHIC_IMPOSSIBLE_TRAVEL", "RULE_AMOUNT_EXCEEDS_5K")
        );

        // 85 * 0.45 + 90 * 0.55 = 38.25 + 49.5 = 87.75 -> 88
        assertEquals(88, eval.getCompositeRiskScore());
        assertEquals(TransactionStatus.BLOCKED, eval.getDecision());
        assertTrue(eval.getDecisionReason().toLowerCase().contains("blocked"));
    }

    @Test
    @DisplayName("Critical override flag should immediately produce 100 risk score and BLOCKED status")
    void testCriticalOverride() {
        CompositeRiskEvaluator.CompositeEvaluation eval = compositeRiskEvaluator.evaluate(
                0, 0, true, List.of("RULE_SANCTIONED_ENTITY")
        );

        assertEquals(100, eval.getCompositeRiskScore());
        assertEquals(TransactionStatus.BLOCKED, eval.getDecision());
    }
}
