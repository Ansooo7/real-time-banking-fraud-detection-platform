package com.ukbank.fraudplatform.service;

import com.ukbank.fraudplatform.dto.MLPredictRequest;
import com.ukbank.fraudplatform.dto.MLPredictResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class MLClientService {

    private final RestClient restClient;
    private final String mlServiceUrl;

    public MLClientService(
            @Value("${ml.service.url:http://localhost:8000}") String mlServiceUrl,
            @Value("${ml.service.connect-timeout-ms:3000}") int connectTimeout,
            @Value("${ml.service.read-timeout-ms:3000}") int readTimeout) {
        
        this.mlServiceUrl = mlServiceUrl;
        
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);

        this.restClient = RestClient.builder()
                .baseUrl(mlServiceUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public MLPredictResponse predictRisk(MLPredictRequest request) {
        try {
            log.debug("Sending ML inference request for tx: {}", request.getTransactionId());
            MLPredictResponse response = restClient.post()
                    .uri("/api/v1/predict")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(MLPredictResponse.class);

            if (response != null) {
                log.debug("Received ML inference response for tx: {}, score: {}", 
                        request.getTransactionId(), response.getRiskScore());
                return response;
            }
        } catch (Exception e) {
            log.warn("ML Service unavailable or failed: {}. Employing fallback heuristic.", e.getMessage());
        }

        return buildFallbackPrediction(request);
    }

    public Map<String, Object> getModelInfo() {
        try {
            return restClient.get()
                    .uri("/api/v1/model/info")
                    .retrieve()
                    .body(Map.class);
        } catch (Exception e) {
            log.warn("Failed to fetch ML model metadata from {}: {}", mlServiceUrl, e.getMessage());
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("status", "UNAVAILABLE");
            fallback.put("message", "ML service offline or loading");
            return fallback;
        }
    }

    private MLPredictResponse buildFallbackPrediction(MLPredictRequest req) {
        double proba = 0.05;
        double ratio = (req.getAvgAmount30d() > 0) ? (req.getAmount() / req.getAvgAmount30d()) : 1.0;
        
        if (ratio > 5.0) proba += 0.35;
        if (req.getTxCount1h() >= 4) proba += 0.30;
        if (req.getGeoDistanceKm() > 500) proba += 0.25;
        if (req.getIsNewDevice() == 1) proba += 0.15;
        
        int score = (int) Math.min(99, Math.round(proba * 100));

        Map<String, Object> factors = new HashMap<>();
        factors.put("fallback_heuristic", true);
        factors.put("amount_ratio", ratio);

        return MLPredictResponse.builder()
                .transactionId(req.getTransactionId())
                .fraudProbability(proba)
                .riskScore(score)
                .modelName("Fallback-Heuristic")
                .modelVersion("1.0.0-fallback")
                .inferenceTimeMs(0.5)
                .riskFactors(factors)
                .build();
    }
}
