package com.ukbank.fraudplatform.dto;

import com.ukbank.fraudplatform.model.Channel;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequest {

    @NotNull(message = "Source account ID is required")
    private UUID sourceAccountId;

    @NotBlank(message = "Destination account number is required")
    private String destinationAccountNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @Builder.Default
    private String currency = "GBP";

    private String merchantCode;

    @NotNull(message = "Transaction channel is required")
    private Channel channel;

    private String deviceFingerprint;
    private String deviceType;
    private String ipAddress;
    private Double latitude;
    private Double longitude;
}
