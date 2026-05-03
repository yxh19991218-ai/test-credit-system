package com.credit.system.event.handler;

import com.credit.system.event.InterestRateChangedEvent;
import com.credit.system.event.DomainEventHandler;
import com.credit.system.service.RepaymentScheduleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class InterestRateChangedHandler implements DomainEventHandler<InterestRateChangedEvent> {

    private static final Logger log = LoggerFactory.getLogger(InterestRateChangedHandler.class);

    private final RepaymentScheduleService scheduleService;

    public InterestRateChangedHandler(RepaymentScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @Override
    public void handle(InterestRateChangedEvent event) {
        Long contractId = event.getContractId();
        log.info("利率变更事件触发还款计划重建，合同ID={}", contractId);

        scheduleService.changeInterestRate(
                contractId,
                event.getNewRate(),
                event.getReason(),
                event.getOperator());
    }

    @Override
    public Class<InterestRateChangedEvent> supportsEventType() {
        return InterestRateChangedEvent.class;
    }
}
