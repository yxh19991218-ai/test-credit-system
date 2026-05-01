package com.credit.system.dto.mapper;

import com.credit.system.domain.LoanContract;
import com.credit.system.dto.ContractResponse;
import org.springframework.stereotype.Component;

/**
 * 合同实体 ↔ DTO 转换映射器。
 */
@Component
public class ContractMapper implements EntityMapper<LoanContract, ContractResponse> {

    @Override
    public ContractResponse toDto(LoanContract c) {
        if (c == null) return null;
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

    @Override
    public LoanContract toEntity(ContractResponse dto) {
        throw new UnsupportedOperationException("ContractResponse 不支持转换为实体，请使用 ContractRequest");
    }
}
