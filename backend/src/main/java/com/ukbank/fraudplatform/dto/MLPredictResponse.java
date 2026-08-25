package com.ukbank.fraudplatform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MLPredictResponse {
    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("fraud_probability")
    private double fraudProbability;

    @JsonProperty("risk_score")
    private int riskScore;

    @JsonProperty("model_name")
    private String modelName;

    @JsonProperty("model_version")
    private String modelVersion;

    @JsonProperty("inference_time_ms")
    private double inferenceTimeMs;

    @JsonProperty("risk_factors")
    private Map<String, Object> riskFactors;
}
