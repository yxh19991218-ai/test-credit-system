package com.credit.system.event;

import lombok.Getter;

/**
 * 贷款合同已创建事件
 * 由履约聚合发布，还款聚合监听并生成还款计划
 */
@Getter
public class ContractCreatedEvent extends DomainEvent {

    private final Long contractId;

    public ContractCreatedEvent(Long contractId) {
        this.contractId = contractId;
    }
}
