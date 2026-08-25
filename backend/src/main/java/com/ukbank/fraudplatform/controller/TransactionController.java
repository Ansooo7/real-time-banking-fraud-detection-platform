package com.ukbank.fraudplatform.controller;

import com.ukbank.fraudplatform.dto.ApiResponse;
import com.ukbank.fraudplatform.dto.PageResponse;
import com.ukbank.fraudplatform.dto.TransactionRequest;
import com.ukbank.fraudplatform.dto.TransactionResponse;
import com.ukbank.fraudplatform.model.TransactionStatus;
import com.ukbank.fraudplatform.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Initiate and query banking transactions with real-time fraud scoring")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    @Operation(summary = "Submit a banking transaction for real-time risk scoring and execution")
    public ResponseEntity<ApiResponse<TransactionResponse>> processTransaction(
            @Valid @RequestBody TransactionRequest request,
            @Parameter(description = "Unique UUID idempotency key to prevent duplicate processing")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        log.info("Incoming transaction request for amount: £{} (Idempotency-Key: {})", request.getAmount(), idempotencyKey);
        TransactionResponse response = transactionService.processTransaction(request, idempotencyKey);
        
        HttpStatus status = (response.getStatus() == TransactionStatus.BLOCKED) 
                ? HttpStatus.OK 
                : HttpStatus.CREATED;

        return ResponseEntity.status(status).body(ApiResponse.ok(response, "Transaction evaluated: " + response.getStatus()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transaction details by ID")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransactionById(@PathVariable UUID id) {
        TransactionResponse response = transactionService.getTransactionById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping
    @Operation(summary = "Search and filter transactions with pagination")
    public ResponseEntity<ApiResponse<PageResponse<TransactionResponse>>> getTransactions(
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {

        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<TransactionResponse> pageResult = transactionService.getTransactions(status, startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(pageResult)));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get transaction history for a specific customer")
    public ResponseEntity<ApiResponse<PageResponse<TransactionResponse>>> getCustomerTransactions(
            @PathVariable UUID customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<TransactionResponse> pageResult = transactionService.getCustomerTransactions(customerId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(pageResult)));
    }
}
