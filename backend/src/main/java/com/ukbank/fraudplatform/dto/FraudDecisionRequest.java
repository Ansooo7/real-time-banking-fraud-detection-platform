package com.ukbank.fraudplatform.dto;

import com.ukbank.fraudplatform.model.AlertStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudDecisionRequest {
    @NotNull(message = "Decision status is required")
    private AlertStatus decision; // CONFIRMED_FRAUD, FALSE_POSITIVE, DISMISSED

    private String notes;
}
