package com.ukbank.fraudplatform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ukbank.fraudplatform.model.AuditLog;
import com.ukbank.fraudplatform.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Async
    public void logAction(String entityType, String entityId, String action, Object beforeState, Object afterState) {
        try {
            String actor = "SYSTEM";
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                actor = auth.getName();
            }

            String correlationId = MDC.get("correlationId");

            String beforeJson = beforeState != null ? objectMapper.writeValueAsString(beforeState) : null;
            String afterJson = afterState != null ? objectMapper.writeValueAsString(afterState) : null;

            AuditLog logEntry = AuditLog.builder()
                    .entityType(entityType)
                    .entityId(entityId)
                    .action(action)
                    .actorUsername(actor)
                    .correlationId(correlationId)
                    .beforeState(beforeJson)
                    .afterState(afterJson)
                    .build();

            auditLogRepository.save(logEntry);
            log.debug("Audit record logged for {} {} by {}", entityType, entityId, actor);
        } catch (Exception e) {
            log.error("Failed to write audit log entry: {}", e.getMessage());
        }
    }
}
