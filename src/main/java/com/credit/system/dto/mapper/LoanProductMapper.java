package com.credit.system.dto.mapper;

import com.credit.system.domain.LoanProduct;
import com.credit.system.dto.LoanProductResponse;
import org.springframework.stereotype.Component;

/**
 * 贷款产品实体 ↔ DTO 转换映射器。
 */
@Component
public class LoanProductMapper implements EntityMapper<LoanProduct, LoanProductResponse> {

    @Override
    public LoanProductResponse toDto(LoanProduct p) {
        if (p == null) return null;
        LoanProductResponse r = new LoanProductResponse();
        r.setId(p.getId());
        r.setProductCode(p.getProductCode());
        r.setProductName(p.getProductName());
        r.setProductDescription(p.getProductDescription());
        r.setStatus(p.getStatus() != null ? p.getStatus().name() : null);
        r.setInterestRate(p.getInterestRate());
        r.setInterestRateType(p.getInterestRateType());
        r.setRepaymentMethod(p.getRepaymentMethod() != null ? p.getRepaymentMethod().name() : null);
        r.setMinAmount(p.getMinAmount());
        r.setMaxAmount(p.getMaxAmount());
        r.setMinTerm(p.getMinTerm());
        r.setMaxTerm(p.getMaxTerm());
        r.setMinAge(p.getMinAge());
        r.setMaxAge(p.getMaxAge());
        r.setMinCreditScore(p.getMinCreditScore());
        r.setMinMonthlyIncome(p.getMinMonthlyIncome());
        r.setServiceFeeRate(p.getServiceFeeRate());
        r.setHandlingFee(p.getHandlingFee());
        r.setValidFrom(p.getValidFrom());
        r.setValidTo(p.getValidTo());
        r.setCreatedBy(p.getCreatedBy());
        r.setCreatedAt(p.getCreatedAt());
        return r;
    }

    @Override
    public LoanProduct toEntity(LoanProductResponse dto) {
        throw new UnsupportedOperationException("LoanProductResponse 不支持转换为实体，请使用 LoanProductRequest");
    }
}
