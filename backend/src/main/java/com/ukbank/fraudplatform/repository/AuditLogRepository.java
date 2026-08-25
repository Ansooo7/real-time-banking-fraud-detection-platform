package com.ukbank.fraudplatform.repository;

import com.ukbank.fraudplatform.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findByEntityType(String entityType, Pageable pageable);
    List<AuditLog> findByCorrelationId(String correlationId);
    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
