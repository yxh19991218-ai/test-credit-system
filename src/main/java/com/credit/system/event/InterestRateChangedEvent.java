package com.credit.system.event;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class InterestRateChangedEvent extends DomainEvent {

    private final Long contractId;
    private final Long applicationId;
    private final BigDecimal oldRate;
    private final BigDecimal newRate;
    private final String changeType;
    private final String reason;
    private final String operator;

    public InterestRateChangedEvent(Long contractId,
                                    Long applicationId,
                                    BigDecimal oldRate,
                                    BigDecimal newRate,
                                    String changeType,
                                    String reason,
                                    String operator) {
        this.contractId = contractId;
        this.applicationId = applicationId;
        this.oldRate = oldRate;
        this.newRate = newRate;
        this.changeType = changeType;
        this.reason = reason;
        this.operator = operator;
    }
}
