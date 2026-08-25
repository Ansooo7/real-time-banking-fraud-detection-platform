package com.ukbank.fraudplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRiskProfileResponse {
    private UUID customerId;
    private String customerNumber;
    private String customerName;
    private String email;
    private String phone;
    private String homeCity;
    private String riskTier;
    private BigDecimal avgTransactionAmount30d;
    private Integer txCountLast24h;
    private Integer overallTrustScore;
    private Integer fraudIncidentCount;
    private String lastKnownIp;
    private ZonedDateTime lastTransactionTime;
    private List<TransactionResponse> recentTransactions;
}
