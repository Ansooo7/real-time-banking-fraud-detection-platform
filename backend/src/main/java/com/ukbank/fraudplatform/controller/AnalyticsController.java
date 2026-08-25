package com.ukbank.fraudplatform.controller;

import com.ukbank.fraudplatform.dto.AnalyticsSummaryResponse;
import com.ukbank.fraudplatform.dto.ApiResponse;
import com.ukbank.fraudplatform.service.AnalyticsService;
import com.ukbank.fraudplatform.service.MLClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/fraud/analytics")
@RequiredArgsConstructor
@Tag(name = "Fraud Analytics & Model Intelligence", description = "Real-time KPI metrics, volume trends, and ML performance metrics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final MLClientService mlClientService;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('FRAUD_ANALYST', 'ADMIN')")
    @Operation(summary = "Get aggregated 24h fraud KPIs, blocked volume, and risk distribution")
    public ResponseEntity<ApiResponse<AnalyticsSummaryResponse>> getSummary() {
        AnalyticsSummaryResponse summary = analyticsService.getAnalyticsSummary();
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }

    @GetMapping("/model-performance")
    @PreAuthorize("hasAnyRole('FRAUD_ANALYST', 'ADMIN')")
    @Operation(summary = "Get ML model governance metadata, PR-AUC, confusion matrix and feature importances")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getModelPerformance() {
        Map<String, Object> modelInfo = mlClientService.getModelInfo();
        return ResponseEntity.ok(ApiResponse.ok(modelInfo));
    }
}
