package com.ukbank.fraudplatform.controller;

import com.ukbank.fraudplatform.dto.ApiResponse;
import com.ukbank.fraudplatform.dto.CustomerRiskProfileResponse;
import com.ukbank.fraudplatform.dto.TransactionResponse;
import com.ukbank.fraudplatform.exception.ResourceNotFoundException;
import com.ukbank.fraudplatform.model.Customer;
import com.ukbank.fraudplatform.model.RiskProfile;
import com.ukbank.fraudplatform.model.Transaction;
import com.ukbank.fraudplatform.repository.CustomerRepository;
import com.ukbank.fraudplatform.repository.RiskProfileRepository;
import com.ukbank.fraudplatform.repository.TransactionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/fraud/risk-profiles")
@RequiredArgsConstructor
@Tag(name = "Customer Risk Profiles", description = "Customer behavioral risk profiling and trust scores")
public class RiskProfileController {

    private final CustomerRepository customerRepository;
    private final RiskProfileRepository riskProfileRepository;
    private final TransactionRepository transactionRepository;

    @GetMapping("/{customerId}")
    @PreAuthorize("hasAnyRole('FRAUD_ANALYST', 'ADMIN')")
    @Operation(summary = "Get deep customer risk intelligence profile")
    public ResponseEntity<ApiResponse<CustomerRiskProfileResponse>> getRiskProfile(@PathVariable UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + customerId));

        RiskProfile profile = riskProfileRepository.findById(customerId).orElse(null);
        List<Transaction> recentTx = transactionRepository.findTop50ByCustomerIdOrderByCreatedAtDesc(customerId);

        List<TransactionResponse> txResponses = recentTx.stream().map(tx -> TransactionResponse.builder()
                .id(tx.getId())
                .sourceAccountId(tx.getSourceAccount().getId())
                .sourceAccountNumber(tx.getSourceAccount().getAccountNumber())
                .destinationAccountNumber(tx.getDestinationAccountNumber())
                .merchantName(tx.getMerchant() != null ? tx.getMerchant().getMerchantName() : "P2P Transfer")
                .amount(tx.getAmount())
                .currency(tx.getCurrency())
                .channel(tx.getChannel())
                .status(tx.getStatus())
                .riskScore(tx.getRiskScore())
                .decisionReason(tx.getDecisionReason())
                .createdAt(tx.getCreatedAt())
                .build()
        ).collect(Collectors.toList());

        CustomerRiskProfileResponse response = CustomerRiskProfileResponse.builder()
                .customerId(customer.getId())
                .customerNumber(customer.getCustomerNumber())
                .customerName(customer.getFirstName() + " " + customer.getLastName())
                .email(customer.getEmail())
                .phone(customer.getPhoneNumber())
                .homeCity(customer.getHomeCity())
                .riskTier(customer.getRiskTier())
                .avgTransactionAmount30d(profile != null ? profile.getAvgTransactionAmount30d() : BigDecimal.ZERO)
                .txCountLast24h(profile != null ? profile.getTxCountLast24h() : 0)
                .overallTrustScore(profile != null ? profile.getOverallTrustScore() : 95)
                .fraudIncidentCount(profile != null ? profile.getFraudIncidentCount() : 0)
                .lastKnownIp(profile != null ? profile.getLastKnownIp() : "N/A")
                .lastTransactionTime(profile != null ? profile.getLastTransactionTime() : null)
                .recentTransactions(txResponses)
                .build();

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
