package com.credit.system.dto;

import lombok.Data;

@Data
public class PageRequestDTO {
    private int page = 0;
    private int size = 10;
    private String name;
    private String phone;
    private String idCard;
    private String keyword;
    private String status;
    private String riskLevel;
}
