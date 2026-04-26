package com.credit.system.dto;

import com.credit.system.domain.LoanProduct;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class LoanProductResponse {
    private Long id;
    private String productCode;
    private String productName;
    private String productDescription;
    private String status;
    private BigDecimal interestRate;
    private String interestRateType;
    private String repaymentMethod;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private Integer minTerm;
    private Integer maxTerm;
    private Integer minAge;
    private Integer maxAge;
    private Integer minCreditScore;
    private BigDecimal minMonthlyIncome;
    private BigDecimal serviceFeeRate;
    private BigDecimal handlingFee;
    private LocalDate validFrom;
    private LocalDate validTo;
    private String createdBy;
    private LocalDateTime createdAt;

    public static LoanProductResponse from(LoanProduct p) {
        LoanProductResponse r = new LoanProductResponse();
        r.setId(p.getId());
        r.setProductCode(p.getProductCode());
        r.setProductName(p.getProductName());
        r.setProductDescription(p.getProductDescription());
        r.setStatus(p.getStatus() != null ? p.getStatus().name() : null);
        r.setInterestRate(p.getInterestRate());
        r.setInterestRateType(p.getInterestRateType());
        r.setRepaymentMethod(p.getRepaymentMethod() != null ? p.getRepaymentMethod().name() : null);
        r.setMinAmount(p.getMinAmount());
        r.setMaxAmount(p.getMaxAmount());
        r.setMinTerm(p.getMinTerm());
        r.setMaxTerm(p.getMaxTerm());
        r.setMinAge(p.getMinAge());
        r.setMaxAge(p.getMaxAge());
        r.setMinCreditScore(p.getMinCreditScore());
        r.setMinMonthlyIncome(p.getMinMonthlyIncome());
        r.setServiceFeeRate(p.getServiceFeeRate());
        r.setHandlingFee(p.getHandlingFee());
        r.setValidFrom(p.getValidFrom());
        r.setValidTo(p.getValidTo());
        r.setCreatedBy(p.getCreatedBy());
        r.setCreatedAt(p.getCreatedAt());
        return r;
    }
}
