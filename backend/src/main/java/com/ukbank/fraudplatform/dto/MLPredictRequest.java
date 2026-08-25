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
public class MLPredictRequest {
    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("customer_id")
    private String customerId;

    private double amount;

    @JsonProperty("avg_amount_30d")
    private double avgAmount30d;

    @JsonProperty("amount_to_avg_ratio")
    private Double amountToAvgRatio;

    @JsonProperty("merchant_risk_base")
    private int merchantRiskBase;

    @JsonProperty("geo_distance_km")
    private double geoDistanceKm;

    @JsonProperty("tx_count_1h")
    private int txCount1h;

    @JsonProperty("tx_count_24h")
    private int txCount24h;

    @JsonProperty("hour_of_day")
    private int hourOfDay;

    @JsonProperty("is_new_device")
    private int isNewDevice;

    @JsonProperty("is_night_time")
    private Integer isNightTime;

    private String channel;
}
