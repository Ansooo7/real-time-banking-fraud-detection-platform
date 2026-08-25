package com.ukbank.fraudplatform.repository;

import com.ukbank.fraudplatform.model.RiskProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RiskProfileRepository extends JpaRepository<RiskProfile, UUID> {
}
