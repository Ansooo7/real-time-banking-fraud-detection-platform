package com.ukbank.fraudplatform.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "devices", uniqueConstraints = {
    @UniqueConstraint(name = "uq_customer_device", columnNames = {"customer_id", "device_fingerprint"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonIgnore
    private Customer customer;

    @Column(nullable = false, length = 128, name = "device_fingerprint")
    private String deviceFingerprint;

    @Column(nullable = false, length = 32, name = "device_type")
    private String deviceType;

    @Column(nullable = false, length = 45, name = "ip_address")
    private String ipAddress;

    @Column(length = 64, name = "location_city")
    private String locationCity;

    @Builder.Default
    @Column(length = 2, name = "location_country")
    private String locationCountry = "GB";

    @Builder.Default
    @Column(nullable = false, name = "is_trusted")
    private Boolean isTrusted = true;

    @CreationTimestamp
    @Column(name = "first_seen_at", updatable = false)
    private ZonedDateTime firstSeenAt;

    @UpdateTimestamp
    @Column(name = "last_seen_at")
    private ZonedDateTime lastSeenAt;
}
