package com.credit.system.dto;

import lombok.Data;

@Data
public class CustomerCreditRequest {
    private Integer creditScore;
    private String creditReportNo;
}
