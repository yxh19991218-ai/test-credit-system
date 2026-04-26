package com.credit.system.dto;

import lombok.Data;

@Data
public class CustomerStatusRequest {
    private String status;
    private String reason;
    private String operator;
}
