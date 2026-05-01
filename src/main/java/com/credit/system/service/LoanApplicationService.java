package com.credit.system.service;

import com.credit.system.domain.LoanApplication;
import com.credit.system.domain.enums.ApplicationStatus;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.Optional;

public interface LoanApplicationService {

    LoanApplication createApplication(LoanApplication application, String operator);

    LoanApplication updateApplication(Long id, LoanApplication applicationDetails);

    Optional<LoanApplication> getApplicationById(Long id);

    Page<LoanApplication> getApplicationsByCustomerId(Long customerId, int page, int size);

    Page<LoanApplication> getApplicationList(Long customerId, Long productId,
                                              ApplicationStatus status, int page, int size);

    void submitApplication(Long id);

    void reviewApplication(Long id, ApplicationStatus decision, String reviewer,
                           String comments, BigDecimal approvedAmount,
                           Integer approvedTerm, BigDecimal interestRate);

    void cancelApplication(Long id, String reason);
}
