package com.ukbank.fraudplatform.event;

import com.ukbank.fraudplatform.model.Channel;
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
public class TransactionCreatedEvent {
    private String eventId;
    private String eventType;
    private String correlationId;
    private ZonedDateTime timestamp;
    
    private UUID transactionId;
    private UUID customerId;
    private UUID sourceAccountId;
    private String destinationAccountNumber;
    private BigDecimal amount;
    private String currency;
    private String mcc;
    private String merchantCode;
    private Channel channel;
    private String deviceFingerprint;
    private String ipAddress;
    private Double latitude;
    private Double longitude;
}
