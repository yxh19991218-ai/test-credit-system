package com.credit.system.dto;

import com.credit.system.domain.LoanProduct;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class LoanProductRequest {
    private String productCode;
    private String productName;
    private String productDescription;
    private BigDecimal interestRate;
    private BigDecimal minInterestRate;
    private BigDecimal maxInterestRate;
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
    private String allowedOccupations;
    private String allowedRegions;
    private BigDecimal serviceFeeRate;
    private BigDecimal handlingFee;
    private BigDecimal managementFeeRate;
    private BigDecimal riskReserveRate;
    private LocalDate validFrom;
    private LocalDate validTo;

    public LoanProduct toEntity() {
        LoanProduct p = new LoanProduct();
        p.setProductCode(productCode);
        p.setProductName(productName);
        p.setProductDescription(productDescription);
        p.setInterestRate(interestRate);
        p.setMinInterestRate(minInterestRate);
        p.setMaxInterestRate(maxInterestRate);
        p.setInterestRateType(interestRateType);
        if (repaymentMethod != null) {
            p.setRepaymentMethod(com.credit.system.domain.enums.RepaymentMethod.valueOf(repaymentMethod));
        }
        p.setMinAmount(minAmount);
        p.setMaxAmount(maxAmount);
        p.setMinTerm(minTerm);
        p.setMaxTerm(maxTerm);
        p.setMinAge(minAge);
        p.setMaxAge(maxAge);
        p.setMinCreditScore(minCreditScore);
        p.setMinMonthlyIncome(minMonthlyIncome);
        p.setAllowedOccupations(allowedOccupations);
        p.setAllowedRegions(allowedRegions);
        p.setServiceFeeRate(serviceFeeRate);
        p.setHandlingFee(handlingFee);
        p.setManagementFeeRate(managementFeeRate);
        p.setRiskReserveRate(riskReserveRate);
        p.setValidFrom(validFrom);
        p.setValidTo(validTo);
        return p;
    }
}
