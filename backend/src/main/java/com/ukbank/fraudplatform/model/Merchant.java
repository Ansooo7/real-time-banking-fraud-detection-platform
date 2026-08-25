package com.ukbank.fraudplatform.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "merchants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 64, name = "merchant_code")
    private String merchantCode;

    @Column(nullable = false, length = 128, name = "merchant_name")
    private String merchantName;

    @Column(nullable = false, length = 4)
    private String mcc; // ISO 18245 Merchant Category Code

    @Column(nullable = false, length = 64, name = "category_name")
    private String categoryName;

    @Builder.Default
    @Column(name = "risk_score_base")
    private Integer riskScoreBase = 10;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;
}
