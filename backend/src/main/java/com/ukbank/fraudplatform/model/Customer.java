package com.ukbank.fraudplatform.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 32, name = "customer_number")
    private String customerNumber;

    @Column(nullable = false, length = 64, name = "first_name")
    private String firstName;

    @Column(nullable = false, length = 64, name = "last_name")
    private String lastName;

    @Column(nullable = false, unique = true, length = 128)
    private String email;

    @Column(nullable = false, length = 32, name = "phone_number")
    private String phoneNumber;

    @Builder.Default
    @Column(length = 64, name = "home_city")
    private String homeCity = "London";

    @Builder.Default
    @Column(length = 2, name = "home_country")
    private String homeCountry = "GB";

    @Builder.Default
    @Column(length = 16, name = "risk_tier")
    private String riskTier = "LOW";

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Account> accounts = new ArrayList<>();

    @OneToOne(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private RiskProfile riskProfile;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;
}
