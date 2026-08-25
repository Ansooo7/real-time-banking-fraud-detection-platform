package com.ukbank.fraudplatform.dto;

import com.ukbank.fraudplatform.model.AlertStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudAlertResponse {
    private UUID id;
    private UUID transactionId;
    private UUID customerId;
    private String customerName;
    private String customerNumber;
    private BigDecimal amount;
    private String currency;
    private String merchantName;
    private Integer ruleScore;
    private Integer mlScore;
    private Integer compositeRiskScore;
    private String triggeredRules;
    private String mlFeatureContributions;
    private AlertStatus status;
    private String assignedAnalyst;
    private String analystNotes;
    private ZonedDateTime createdAt;
    private ZonedDateTime resolvedAt;
}
