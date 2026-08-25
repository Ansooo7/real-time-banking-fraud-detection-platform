package com.ukbank.fraudplatform.controller;

import com.ukbank.fraudplatform.dto.ApiResponse;
import com.ukbank.fraudplatform.dto.PageResponse;
import com.ukbank.fraudplatform.model.AuditLog;
import com.ukbank.fraudplatform.repository.AuditLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Compliance & Audit Logs", description = "Immutable audit trails for banking compliance officers")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get immutable system audit logs with pagination")
    public ResponseEntity<ApiResponse<PageResponse<AuditLog>>> getAuditLogs(
            @RequestParam(required = false) String entityType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLog> result = (entityType != null && !entityType.isBlank())
                ? auditLogRepository.findByEntityType(entityType, pageable)
                : auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);

        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(result)));
    }
}
