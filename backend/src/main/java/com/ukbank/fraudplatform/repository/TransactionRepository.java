package com.ukbank.fraudplatform.repository;

import com.ukbank.fraudplatform.model.Transaction;
import com.ukbank.fraudplatform.model.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
    
    @Query("SELECT t FROM Transaction t WHERE t.sourceAccount.customer.id = :customerId ORDER BY t.createdAt DESC")
    Page<Transaction> findByCustomerId(@Param("customerId") UUID customerId, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.sourceAccount.customer.id = :customerId ORDER BY t.createdAt DESC")
    List<Transaction> findTop50ByCustomerIdOrderByCreatedAtDesc(@Param("customerId") UUID customerId);

    Page<Transaction> findByStatus(TransactionStatus status, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:startDate IS NULL OR t.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR t.createdAt <= :endDate)")
    Page<Transaction> findFiltered(
            @Param("status") TransactionStatus status,
            @Param("startDate") ZonedDateTime startDate,
            @Param("endDate") ZonedDateTime endDate,
            Pageable pageable
    );

    long countByCreatedAtAfter(ZonedDateTime dateTime);
    long countByStatusAndCreatedAtAfter(TransactionStatus status, ZonedDateTime dateTime);
}
