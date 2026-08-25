package com.ukbank.fraudplatform.repository;

import com.ukbank.fraudplatform.model.AlertStatus;
import com.ukbank.fraudplatform.model.FraudAlert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, UUID> {
    Optional<FraudAlert> findByTransactionId(UUID transactionId);
    Page<FraudAlert> findByStatus(AlertStatus status, Pageable pageable);
    List<FraudAlert> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    @Query("SELECT fa FROM FraudAlert fa WHERE " +
           "(:status IS NULL OR fa.status = :status) " +
           "ORDER BY fa.createdAt DESC")
    Page<FraudAlert> findFiltered(@Param("status") AlertStatus status, Pageable pageable);

    long countByStatus(AlertStatus status);
}
