package com.credit.system.dto;

import com.credit.system.domain.Customer;
import com.credit.system.domain.enums.CustomerStatus;
import com.credit.system.domain.enums.RiskLevel;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CustomerRequest {
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

    public Customer toEntity() {
        Customer c = new Customer();
        c.setName(this.name);
        c.setIdCard(this.idCard);
        c.setPhone(this.phone);
        c.setEmail(this.email);
        c.setOccupation(this.occupation);
        c.setMonthlyIncome(this.monthlyIncome);
        c.setAddress(this.address);
        c.setCreditScore(this.creditScore);
        c.setCreditReportNo(this.creditReportNo);
        c.setBankCardNo(this.bankCardNo);
        c.setBankName(this.bankName);
        c.setEmergencyContactName(this.emergencyContactName);
        c.setEmergencyContactPhone(this.emergencyContactPhone);
        return c;
    }
}
