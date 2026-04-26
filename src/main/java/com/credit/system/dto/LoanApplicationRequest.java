package com.credit.system.dto;

import com.credit.system.domain.LoanApplication;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class LoanApplicationRequest {
    private Long customerId;
    private Long productId;
    private BigDecimal applyAmount;
    private Integer applyTerm;
    private String purpose;

    public LoanApplication toEntity() {
        LoanApplication app = new LoanApplication();
        app.setCustomerId(customerId);
        app.setProductId(productId);
        app.setApplyAmount(applyAmount);
        app.setApplyTerm(applyTerm);
        app.setPurpose(purpose);
        return app;
    }
}
