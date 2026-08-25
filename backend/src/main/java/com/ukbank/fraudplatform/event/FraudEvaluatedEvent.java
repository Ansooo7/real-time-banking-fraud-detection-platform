package com.ukbank.fraudplatform.event;

import com.ukbank.fraudplatform.model.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudEvaluatedEvent {
    private String eventId;
    private UUID transactionId;
    private UUID customerId;
    private Integer ruleScore;
    private Integer mlScore;
    private Integer compositeRiskScore;
    private TransactionStatus decision;
    private List<String> triggeredRules;
    private ZonedDateTime timestamp;
}
