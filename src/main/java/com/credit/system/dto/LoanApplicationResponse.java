package com.credit.system.dto;

import com.credit.system.domain.LoanApplication;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class LoanApplicationResponse {
    private Long id;
    private Long customerId;
    private Long productId;
    private BigDecimal applyAmount;
    private Integer applyTerm;
    private String purpose;
    private String status;
    private BigDecimal approvedAmount;
    private Integer approvedTerm;
    private BigDecimal interestRate;
    private BigDecimal monthlyPayment;
    private LocalDateTime applicationDate;
    private LocalDateTime submitDate;
    private String reviewer;
    private String reviewComments;
    private LocalDateTime createdAt;

    public static LoanApplicationResponse from(LoanApplication app) {
        LoanApplicationResponse r = new LoanApplicationResponse();
        r.setId(app.getId());
        r.setCustomerId(app.getCustomerId());
        r.setProductId(app.getProductId());
        r.setApplyAmount(app.getApplyAmount());
        r.setApplyTerm(app.getApplyTerm());
        r.setPurpose(app.getPurpose());
        r.setStatus(app.getStatus() != null ? app.getStatus().name() : null);
        r.setApprovedAmount(app.getApprovedAmount());
        r.setApprovedTerm(app.getApprovedTerm());
        r.setInterestRate(app.getInterestRate());
        r.setMonthlyPayment(app.getMonthlyPayment());
        r.setApplicationDate(app.getApplicationDate());
        r.setSubmitDate(app.getSubmitDate());
        r.setReviewer(app.getReviewer());
        r.setReviewComments(app.getReviewComments());
        r.setCreatedAt(app.getCreatedAt());
        return r;
    }
}
