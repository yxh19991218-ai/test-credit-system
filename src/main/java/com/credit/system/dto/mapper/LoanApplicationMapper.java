package com.credit.system.dto.mapper;

import com.credit.system.domain.LoanApplication;
import com.credit.system.dto.LoanApplicationResponse;
import org.springframework.stereotype.Component;

/**
 * 贷款申请实体 ↔ DTO 转换映射器。
 */
@Component
public class LoanApplicationMapper implements EntityMapper<LoanApplication, LoanApplicationResponse> {

    @Override
    public LoanApplicationResponse toDto(LoanApplication app) {
        if (app == null) return null;
        LoanApplicationResponse r = new LoanApplicationResponse();
        r.setId(app.getId());
        r.setCustomerId(app.getCustomerId());
        r.setProductId(app.getProductId());
        r.setApplyAmount(app.getApplyAmount());
        r.setApplyTerm(app.getApplyTerm());
        r.setPurpose(app.getPurpose());
        r.setStatus(app.getStatus() != null ? app.getStatus().name() : null);
        r.setApprovedAmount(app.getApprovedAmount());
        r.setApprovedTerm(app.getApprovedTerm());
        r.setInterestRate(app.getInterestRate());
        r.setMonthlyPayment(app.getMonthlyPayment());
        r.setApplicationDate(app.getApplicationDate());
        r.setSubmitDate(app.getSubmitDate());
        r.setReviewer(app.getReviewer());
        r.setReviewComments(app.getReviewComments());
        r.setCreatedAt(app.getCreatedAt());
        return r;
    }

    @Override
    public LoanApplication toEntity(LoanApplicationResponse dto) {
        throw new UnsupportedOperationException("LoanApplicationResponse 不支持转换为实体，请使用 LoanApplicationRequest");
    }
}
