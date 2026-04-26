package com.credit.system.dto;

import com.credit.system.domain.Customer;
import com.credit.system.domain.enums.CustomerStatus;
import com.credit.system.domain.enums.RiskLevel;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CustomerResponse {
    private Long id;
    private String name;
    private String idCard;
    private String phone;
    private String email;
    private String occupation;
    private BigDecimal monthlyIncome;
    private String address;
    private Integer creditScore;
    private String creditReportNo;
    private String bankCardNo;
    private String bankName;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private CustomerStatus status;
    private String statusReason;
    private RiskLevel riskLevel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CustomerResponse from(Customer c) {
        CustomerResponse r = new CustomerResponse();
        r.setId(c.getId());
        r.setName(c.getName());
        r.setIdCard(c.getIdCard());
        r.setPhone(c.getPhone());
        r.setEmail(c.getEmail());
        r.setOccupation(c.getOccupation());
        r.setMonthlyIncome(c.getMonthlyIncome());
        r.setAddress(c.getAddress());
        r.setCreditScore(c.getCreditScore());
        r.setCreditReportNo(c.getCreditReportNo());
        r.setBankCardNo(c.getBankCardNo());
        r.setBankName(c.getBankName());
        r.setEmergencyContactName(c.getEmergencyContactName());
        r.setEmergencyContactPhone(c.getEmergencyContactPhone());
        r.setStatus(c.getStatus());
        r.setStatusReason(c.getStatusReason());
        r.setRiskLevel(c.getRiskLevel());
        r.setCreatedAt(c.getCreatedAt());
        r.setUpdatedAt(c.getUpdatedAt());
        return r;
    }
}
