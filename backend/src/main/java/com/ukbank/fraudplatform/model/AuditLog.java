package com.ukbank.fraudplatform.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false, length = 32)
    private String entityType; // TRANSACTION, FRAUD_ALERT, USER, CONFIG

    @Column(name = "entity_id", nullable = false, length = 64)
    private String entityId;

    @Column(nullable = false, length = 32)
    private String action; // CREATED, EVALUATED, STATUS_CHANGED, OVERRIDDEN

    @Column(name = "actor_username", nullable = false, length = 64)
    private String actorUsername;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "before_state", columnDefinition = "TEXT")
    private String beforeState;

    @Column(name = "after_state", columnDefinition = "TEXT")
    private String afterState;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;
}
