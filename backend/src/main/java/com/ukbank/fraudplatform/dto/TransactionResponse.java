package com.ukbank.fraudplatform.dto;

import com.ukbank.fraudplatform.model.Channel;
import com.ukbank.fraudplatform.model.TransactionStatus;
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
public class TransactionResponse {
    private UUID id;
    private String idempotencyKey;
    private UUID sourceAccountId;
    private String sourceAccountNumber;
    private String customerName;
    private String destinationAccountNumber;
    private String merchantName;
    private String merchantCategory;
    private BigDecimal amount;
    private String currency;
    private Channel channel;
    private TransactionStatus status;
    private Integer riskScore;
    private String decisionReason;
    private String ipAddress;
    private Double latitude;
    private Double longitude;
    private ZonedDateTime createdAt;
}
