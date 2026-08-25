package com.ukbank.fraudplatform.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "fraud_alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "rule_score", nullable = false)
    private Integer ruleScore;

    @Column(name = "ml_score", nullable = false)
    private Integer mlScore;

    @Column(name = "composite_risk_score", nullable = false)
    private Integer compositeRiskScore;

    @Column(name = "triggered_rules", columnDefinition = "TEXT")
    private String triggeredRules;

    @Column(name = "ml_feature_contributions", columnDefinition = "TEXT")
    private String mlFeatureContributions;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", length = 24)
    private AlertStatus status = AlertStatus.PENDING_REVIEW;

    @Column(name = "assigned_analyst", length = 64)
    private String assignedAnalyst;

    @Column(name = "analyst_notes", columnDefinition = "TEXT")
    private String analystNotes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "resolved_at")
    private ZonedDateTime resolvedAt;
}
