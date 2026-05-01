package com.credit.system.dto.mapper;

import com.credit.system.domain.Customer;
import com.credit.system.dto.CustomerResponse;
import org.springframework.stereotype.Component;

/**
 * 客户实体 ↔ DTO 转换映射器。
 */
@Component
public class CustomerMapper implements EntityMapper<Customer, CustomerResponse> {

    @Override
    public CustomerResponse toDto(Customer c) {
        if (c == null) return null;
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

    @Override
    public Customer toEntity(CustomerResponse dto) {
        // CustomerResponse 是只读响应，不支持反向转换
        throw new UnsupportedOperationException("CustomerResponse 不支持转换为实体，请使用 CustomerRequest");
    }
}
