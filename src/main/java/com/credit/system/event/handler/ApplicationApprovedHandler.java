package com.credit.system.event.handler;

import com.credit.system.event.ApplicationApprovedEvent;
import com.credit.system.event.DomainEventHandler;
import com.credit.system.service.ContractService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 审批通过事件处理器。
 * <p>
 * 审批通过后，自动创建贷款合同。
 * </p>
 */
@Component
public class ApplicationApprovedHandler implements DomainEventHandler<ApplicationApprovedEvent> {

    private static final Logger log = LoggerFactory.getLogger(ApplicationApprovedHandler.class);

    private final ContractService contractService;

    public ApplicationApprovedHandler(ContractService contractService) {
        this.contractService = contractService;
    }

    @Override
    public void handle(ApplicationApprovedEvent event) {
        log.info("审批通过事件触发合同创建，申请ID={}", event.getApplicationId());
        contractService.createContractFromApplication(event.getApplicationId(), "SYSTEM");
    }

    @Override
    public Class<ApplicationApprovedEvent> supportsEventType() {
        return ApplicationApprovedEvent.class;
    }
}
