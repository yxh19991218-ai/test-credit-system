package com.credit.system.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.credit.system.domain.LoanContract;
import com.credit.system.domain.enums.ContractStatus;

public interface LoanContractRepository extends JpaRepository<LoanContract, Long> {

    Optional<LoanContract> findByContractNo(String contractNo);

    boolean existsByContractNo(String contractNo);

    List<LoanContract> findByCustomerId(Long customerId);

    Page<LoanContract> findByCustomerId(Long customerId, Pageable pageable);

    List<LoanContract> findByProductId(Long productId);

    List<LoanContract> findByStatus(ContractStatus status);

    Optional<LoanContract> findByApplicationId(Long applicationId);

    @Query("SELECT c FROM LoanContract c WHERE c.status = 'ACTIVE' AND c.endDate < :today")
    List<LoanContract> findOverdueContracts(@Param("today") LocalDate today);

    @Query("SELECT c FROM LoanContract c WHERE c.status = 'ACTIVE' AND c.endDate BETWEEN :from AND :to")
    List<LoanContract> findContractsDueBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Modifying
    @Query("UPDATE LoanContract c SET c.status = :status, c.terminationReason = :reason WHERE c.id = :id")
    void terminateContract(@Param("id") Long id,
                          @Param("status") ContractStatus status,
                          @Param("reason") String reason);

    @Modifying
    @Query("UPDATE LoanContract c SET c.extendedMonths = :months, " +
           "c.extensionReason = :reason, c.endDate = :newEndDate WHERE c.id = :id")
    void extendContract(@Param("id") Long id,
                       @Param("months") Integer months,
                       @Param("reason") String reason,
                       @Param("newEndDate") LocalDate newEndDate);

    long countByCustomerIdAndStatus(Long customerId, ContractStatus status);

    @Query("SELECT SUM(c.remainingPrincipal) FROM LoanContract c WHERE c.customerId = :customerId " +
           "AND c.status = 'ACTIVE'")
    BigDecimal sumActiveRemainingPrincipal(@Param("customerId") Long customerId);

    @Query("SELECT SUM(c.remainingPrincipal) FROM LoanContract c WHERE c.status = 'ACTIVE'")
    BigDecimal sumAllActiveRemainingPrincipal();

    @Query("SELECT COUNT(c) > 0 FROM LoanContract c " +
           "WHERE c.customerId = :customerId AND c.status = 'ACTIVE'")
    boolean hasActiveContract(@Param("customerId") Long customerId);

    long countByStatus(ContractStatus status);
}
