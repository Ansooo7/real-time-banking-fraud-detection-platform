package com.ukbank.fraudplatform.controller;

import com.ukbank.fraudplatform.dto.ApiResponse;
import com.ukbank.fraudplatform.dto.FraudAlertResponse;
import com.ukbank.fraudplatform.dto.FraudDecisionRequest;
import com.ukbank.fraudplatform.dto.PageResponse;
import com.ukbank.fraudplatform.exception.ResourceNotFoundException;
import com.ukbank.fraudplatform.model.AlertStatus;
import com.ukbank.fraudplatform.model.FraudAlert;
import com.ukbank.fraudplatform.repository.FraudAlertRepository;
import com.ukbank.fraudplatform.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/fraud/alerts")
@RequiredArgsConstructor
@Tag(name = "Fraud Alerts & Analyst Workbench", description = "Review and action flagged suspicious transactions")
public class FraudAlertController {

    private final FraudAlertRepository fraudAlertRepository;
    private final AuditService auditService;

    @GetMapping
    @PreAuthorize("hasAnyRole('FRAUD_ANALYST', 'ADMIN')")
    @Operation(summary = "Fetch fraud alerts queue with pagination and status filtering")
    public ResponseEntity<ApiResponse<PageResponse<FraudAlertResponse>>> getAlerts(
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<FraudAlert> alerts = fraudAlertRepository.findFiltered(status, pageable);
        
        Page<FraudAlertResponse> responsePage = alerts.map(this::mapToResponse);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(responsePage)));
    }

    @PostMapping("/{id}/decision")
    @PreAuthorize("hasAnyRole('FRAUD_ANALYST', 'ADMIN')")
    @Transactional
    @Operation(summary = "Submit analyst review decision on a fraud alert")
    public ResponseEntity<ApiResponse<FraudAlertResponse>> submitDecision(
            @PathVariable UUID id,
            @Valid @RequestBody FraudDecisionRequest request,
            Authentication authentication) {

        FraudAlert alert = fraudAlertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fraud Alert not found with ID: " + id));

        String analyst = authentication != null ? authentication.getName() : "ANALYST";
        AlertStatus previousStatus = alert.getStatus();

        alert.setStatus(request.getDecision());
        alert.setAssignedAnalyst(analyst);
        alert.setAnalystNotes(request.getNotes());
        alert.setResolvedAt(ZonedDateTime.now());

        alert = fraudAlertRepository.save(alert);

        auditService.logAction("FRAUD_ALERT", alert.getId().toString(), "DECISION_APPLIED", 
                previousStatus, alert.getStatus());

        log.info("Analyst {} resolved alert {} as {}", analyst, id, request.getDecision());

        return ResponseEntity.ok(ApiResponse.ok(mapToResponse(alert), "Alert updated successfully"));
    }

    private FraudAlertResponse mapToResponse(FraudAlert alert) {
        return FraudAlertResponse.builder()
                .id(alert.getId())
                .transactionId(alert.getTransaction().getId())
                .customerId(alert.getCustomer().getId())
                .customerName(alert.getCustomer().getFirstName() + " " + alert.getCustomer().getLastName())
                .customerNumber(alert.getCustomer().getCustomerNumber())
                .amount(alert.getTransaction().getAmount())
                .currency(alert.getTransaction().getCurrency())
                .merchantName(alert.getTransaction().getMerchant() != null 
                        ? alert.getTransaction().getMerchant().getMerchantName() : "P2P Transfer")
                .ruleScore(alert.getRuleScore())
                .mlScore(alert.getMlScore())
                .compositeRiskScore(alert.getCompositeRiskScore())
                .triggeredRules(alert.getTriggeredRules())
                .mlFeatureContributions(alert.getMlFeatureContributions())
                .status(alert.getStatus())
                .assignedAnalyst(alert.getAssignedAnalyst())
                .analystNotes(alert.getAnalystNotes())
                .createdAt(alert.getCreatedAt())
                .resolvedAt(alert.getResolvedAt())
                .build();
    }
}
