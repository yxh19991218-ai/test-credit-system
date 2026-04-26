package com.credit.system.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ApplicationReviewRequest {
    private String decision;       // APPROVED, REJECTED
    private String reviewer;
    private String comments;
    private BigDecimal approvedAmount;
    private Integer approvedTerm;
    private BigDecimal interestRate;
}
