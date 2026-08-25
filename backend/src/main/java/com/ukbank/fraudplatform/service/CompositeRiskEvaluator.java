package com.ukbank.fraudplatform.service;

import com.ukbank.fraudplatform.model.TransactionStatus;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class CompositeRiskEvaluator {

    private static final double RULE_WEIGHT = 0.45;
    private static final double ML_WEIGHT = 0.55;

    public static final int APPROVED_THRESHOLD = 30;
    public static final int REVIEW_THRESHOLD = 70;

    @Data
    @Builder
    public static class CompositeEvaluation {
        private int ruleScore;
        private int mlScore;
        private int compositeRiskScore;
        private TransactionStatus decision;
        private String decisionReason;
    }

    public CompositeEvaluation evaluate(
            int ruleScore,
            int mlScore,
            boolean criticalOverride,
            List<String> triggeredRules) {

        int compositeScore;
        if (criticalOverride) {
            compositeScore = 100;
        } else {
            double weighted = (ruleScore * RULE_WEIGHT) + (mlScore * ML_WEIGHT);
            compositeScore = (int) Math.min(100, Math.max(0, Math.round(weighted)));
        }

        TransactionStatus decision;
        StringBuilder reason = new StringBuilder();

        if (compositeScore <= APPROVED_THRESHOLD) {
            decision = TransactionStatus.APPROVED;
            reason.append("Transaction verified and approved. Low composite risk score (").append(compositeScore).append("/100).");
        } else if (compositeScore <= REVIEW_THRESHOLD) {
            decision = TransactionStatus.REVIEW;
            reason.append("Flagged for analyst review. Medium composite risk score (").append(compositeScore).append("/100).");
            if (!triggeredRules.isEmpty()) {
                reason.append(" Triggers: ").append(String.join(", ", triggeredRules));
            }
        } else {
            decision = TransactionStatus.BLOCKED;
            reason.append("High fraud probability detected. Transaction blocked (Score: ").append(compositeScore).append("/100).");
            if (!triggeredRules.isEmpty()) {
                reason.append(" Triggers: ").append(String.join(", ", triggeredRules));
            }
        }

        log.info("Composite risk evaluated: RuleScore={}, MLScore={}, FinalScore={}, Decision={}",
                ruleScore, mlScore, compositeScore, decision);

        return CompositeEvaluation.builder()
                .ruleScore(ruleScore)
                .mlScore(mlScore)
                .compositeRiskScore(compositeScore)
                .decision(decision)
                .decisionReason(reason.toString())
                .build();
    }
}
