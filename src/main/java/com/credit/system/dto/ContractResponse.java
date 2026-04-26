package com.credit.system.dto;

import com.credit.system.domain.LoanContract;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ContractResponse {
    private Long id;
    private Long applicationId;
    private String contractNo;
    private Long customerId;
    private Long productId;
    private BigDecimal totalAmount;
    private BigDecimal remainingPrincipal;
    private Integer term;
    private Integer paidPeriods;
    private BigDecimal interestRate;
    private String repaymentMethod;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private LocalDateTime signDate;
    private String signatory;
    private String signatureMethod;
    private String terminationReason;
    private Integer extendedMonths;
    private String extensionReason;
    private LocalDateTime createdAt;

    public static ContractResponse from(LoanContract c) {
        ContractResponse r = new ContractResponse();
        r.setId(c.getId());
        r.setApplicationId(c.getApplicationId());
        r.setContractNo(c.getContractNo());
        r.setCustomerId(c.getCustomerId());
        r.setProductId(c.getProductId());
        r.setTotalAmount(c.getTotalAmount());
        r.setRemainingPrincipal(c.getRemainingPrincipal());
        r.setTerm(c.getTerm());
        r.setPaidPeriods(c.getPaidPeriods());
        r.setInterestRate(c.getInterestRate());
        r.setRepaymentMethod(c.getRepaymentMethod() != null ? c.getRepaymentMethod().name() : null);
        r.setStartDate(c.getStartDate());
        r.setEndDate(c.getEndDate());
        r.setStatus(c.getStatus() != null ? c.getStatus().name() : null);
        r.setSignDate(c.getSignDate());
        r.setSignatory(c.getSignatory());
        r.setSignatureMethod(c.getSignatureMethod());
        r.setTerminationReason(c.getTerminationReason());
        r.setExtendedMonths(c.getExtendedMonths());
        r.setExtensionReason(c.getExtensionReason());
        r.setCreatedAt(c.getCreatedAt());
        return r;
    }
}
