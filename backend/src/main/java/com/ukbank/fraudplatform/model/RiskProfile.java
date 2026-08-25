package com.ukbank.fraudplatform.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "risk_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskProfile {

    @Id
    private UUID customerId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "customer_id")
    @JsonIgnore
    private Customer customer;

    @Builder.Default
    @Column(name = "avg_transaction_amount_30d", precision = 15, scale = 2)
    private BigDecimal avgTransactionAmount30d = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "tx_count_last_24h")
    private Integer txCountLast24h = 0;

    @Column(name = "last_known_ip", length = 45)
    private String lastKnownIp;

    @Column(name = "last_known_latitude")
    private Double lastKnownLatitude;

    @Column(name = "last_known_longitude")
    private Double lastKnownLongitude;

    @Column(name = "last_transaction_time")
    private ZonedDateTime lastTransactionTime;

    @Builder.Default
    @Column(name = "fraud_incident_count")
    private Integer fraudIncidentCount = 0;

    @Builder.Default
    @Column(name = "overall_trust_score")
    private Integer overallTrustScore = 95; // 0 to 100

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;
}
