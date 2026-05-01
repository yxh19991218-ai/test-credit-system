package com.credit.system.event.handler;

import com.credit.system.event.ContractCreatedEvent;
import com.credit.system.event.DomainEventHandler;
import com.credit.system.service.RepaymentScheduleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 合同创建事件处理器。
 * <p>
 * 合同创建后，自动生成还款计划。包含幂等性检查，防止重复生成。
 * </p>
 */
@Component
public class ContractCreatedHandler implements DomainEventHandler<ContractCreatedEvent> {

    private static final Logger log = LoggerFactory.getLogger(ContractCreatedHandler.class);

    private final RepaymentScheduleService scheduleService;

    public ContractCreatedHandler(RepaymentScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @Override
    public void handle(ContractCreatedEvent event) {
        Long contractId = event.getContractId();
        log.info("合同已创建事件触发还款计划生成，合同ID={}", contractId);

        // 幂等性检查：已存在还款计划则跳过
        if (scheduleService.getScheduleByContractId(contractId).isEmpty()) {
            scheduleService.generateSchedule(contractId);
        } else {
            log.info("合同ID={} 已存在还款计划，忽略重复生成", contractId);
        }
    }

    @Override
    public Class<ContractCreatedEvent> supportsEventType() {
        return ContractCreatedEvent.class;
    }
}
