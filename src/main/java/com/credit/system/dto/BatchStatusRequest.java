package com.credit.system.dto;

import lombok.Data;
import java.util.List;

@Data
public class BatchStatusRequest {
    private List<Long> customerIds;
    private String status;
    private String reason;
    private String operator;
}
