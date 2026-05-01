package com.credit.system.listener;

import com.credit.system.event.DomainEventWrapper;
import com.credit.system.event.ContractCreatedEvent;
import com.credit.system.event.DomainEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 合同创建事件监听器。
 * <p>
 * 职责仅限于事件路由，业务逻辑委托给 {@link DomainEventHandler}。
 * </p>
 */
@Component
public class ContractCreatedEventListener {

    private static final Logger log = LoggerFactory.getLogger(ContractCreatedEventListener.class);

    private final DomainEventHandler<ContractCreatedEvent> handler;

    public ContractCreatedEventListener(DomainEventHandler<ContractCreatedEvent> handler) {
        this.handler = handler;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onContractCreated(DomainEventWrapper wrapper) {
        if (wrapper.getDomainEvent() instanceof ContractCreatedEvent event) {
            log.debug("路由合同创建事件到处理器，eventId={}", event.getEventId());
            handler.handle(event);
        }
    }
}
