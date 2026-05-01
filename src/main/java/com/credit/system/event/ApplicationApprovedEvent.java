package com.credit.system.event;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 贷款申请审核通过事件
 * 由审批聚合发布，合同聚合监听并创建合同
 */
@Getter
public class ApplicationApprovedEvent extends DomainEvent {

    private final Long applicationId;
    private final Long customerId;
    private final Long productId;
    private final BigDecimal approvedAmount;
    private final Integer approvedTerm;
    private final BigDecimal interestRate;
    private final String reviewer;
    private final LocalDateTime reviewDate;
    private final String reviewComments;

    public ApplicationApprovedEvent(Long applicationId,
                                   Long customerId,
                                   Long productId,
                                   BigDecimal approvedAmount,
                                   Integer approvedTerm,
                                   BigDecimal interestRate,
                                   String reviewer,
                                   LocalDateTime reviewDate,
                                   String reviewComments) {
        this.applicationId = applicationId;
        this.customerId = customerId;
        this.productId = productId;
        this.approvedAmount = approvedAmount;
        this.approvedTerm = approvedTerm;
        this.interestRate = interestRate;
        this.reviewer = reviewer;
        this.reviewDate = reviewDate;
        this.reviewComments = reviewComments;
    }
}
