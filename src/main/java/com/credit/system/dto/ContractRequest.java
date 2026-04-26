package com.credit.system.dto;

import com.credit.system.domain.LoanContract;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ContractRequest {
    private Long applicationId;
    private Long customerId;
    private Long productId;
    private BigDecimal totalAmount;
    private Integer term;
    private BigDecimal interestRate;
    private String repaymentMethod;
    private LocalDate startDate;
    private LocalDate endDate;

    public LoanContract toEntity() {
        LoanContract c = new LoanContract();
        c.setApplicationId(applicationId);
        c.setCustomerId(customerId);
        c.setProductId(productId);
        c.setTotalAmount(totalAmount);
        c.setTerm(term);
        c.setInterestRate(interestRate);
        if (repaymentMethod != null) {
            c.setRepaymentMethod(com.credit.system.domain.enums.RepaymentMethod.valueOf(repaymentMethod));
        }
        c.setStartDate(startDate);
        c.setEndDate(endDate);
        return c;
    }
}
