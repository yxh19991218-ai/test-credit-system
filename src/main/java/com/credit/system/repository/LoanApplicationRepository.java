package com.credit.system.repository;

import com.credit.system.domain.LoanApplication;
import com.credit.system.domain.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {

    List<LoanApplication> findByCustomerId(Long customerId);

    Page<LoanApplication> findByCustomerId(Long customerId, Pageable pageable);

    List<LoanApplication> findByProductId(Long productId);

    List<LoanApplication> findByStatus(ApplicationStatus status);

    Optional<LoanApplication> findByContractId(Long contractId);

    @Query("SELECT a FROM LoanApplication a WHERE a.status = :status ORDER BY a.applicationDate DESC")
    List<LoanApplication> findPendingApplications(@Param("status") ApplicationStatus status);

    @Modifying
    @Query("UPDATE LoanApplication a SET a.status = :status, a.reviewer = :reviewer, " +
           "a.reviewDate = :reviewDate, a.reviewComments = :comments, " +
           "a.approvedAmount = :approvedAmount, a.approvedTerm = :approvedTerm, " +
           "a.interestRate = :interestRate WHERE a.id = :id")
    void reviewApplication(@Param("id") Long id,
                          @Param("status") ApplicationStatus status,
                          @Param("reviewer") String reviewer,
                          @Param("reviewDate") LocalDateTime reviewDate,
                          @Param("comments") String comments,
                          @Param("approvedAmount") java.math.BigDecimal approvedAmount,
                          @Param("approvedTerm") Integer approvedTerm,
                          @Param("interestRate") java.math.BigDecimal interestRate);

    long countByCustomerIdAndStatus(Long customerId, ApplicationStatus status);

    @Query("SELECT COUNT(a) > 0 FROM LoanApplication a " +
           "WHERE a.customerId = :customerId AND a.status IN :statuses")
    boolean hasApplicationsWithStatus(@Param("customerId") Long customerId,
                                      @Param("statuses") List<ApplicationStatus> statuses);
}
