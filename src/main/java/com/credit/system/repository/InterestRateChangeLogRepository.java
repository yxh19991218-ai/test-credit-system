package com.credit.system.repository;

import com.credit.system.domain.InterestRateChangeLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface InterestRateChangeLogRepository extends JpaRepository<InterestRateChangeLog, Long> {

    Page<InterestRateChangeLog> findByContractIdOrderByCreatedAtDesc(Long contractId, Pageable pageable);

    Page<InterestRateChangeLog> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);

    @Query("SELECT COUNT(l) FROM InterestRateChangeLog l " +
           "WHERE l.contractId = :contractId AND l.createdAt > :since")
    long countByContractIdSince(@Param("contractId") Long contractId, @Param("since") LocalDateTime since);
}
