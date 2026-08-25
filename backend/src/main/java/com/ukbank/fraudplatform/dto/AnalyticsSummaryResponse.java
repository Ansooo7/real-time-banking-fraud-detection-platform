package com.ukbank.fraudplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsSummaryResponse {
    private long totalTransactions24h;
    private long approvedCount;
    private long reviewCount;
    private long blockedCount;
    private double fraudRatePercent;
    private BigDecimal totalVolumeGbp;
    private BigDecimal totalBlockedAmountGbp;
    private long openAlertsCount;
    private Map<String, Long> channelDistribution;
    private Map<String, Long> riskTierDistribution;
}
