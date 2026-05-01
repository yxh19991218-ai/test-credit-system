package com.credit.system.listener;

import com.credit.system.event.DomainEventWrapper;
import com.credit.system.event.ApplicationApprovedEvent;
import com.credit.system.event.DomainEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 审批通过事件监听器。
 * <p>
 * 职责仅限于事件路由，业务逻辑委托给 {@link DomainEventHandler}。
 * </p>
 */
@Component
public class ApplicationApprovedEventListener {

    private static final Logger log = LoggerFactory.getLogger(ApplicationApprovedEventListener.class);

    private final DomainEventHandler<ApplicationApprovedEvent> handler;

    public ApplicationApprovedEventListener(DomainEventHandler<ApplicationApprovedEvent> handler) {
        this.handler = handler;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onApplicationApproved(DomainEventWrapper wrapper) {
        if (wrapper.getDomainEvent() instanceof ApplicationApprovedEvent event) {
            log.debug("路由审批通过事件到处理器，eventId={}", event.getEventId());
            handler.handle(event);
        }
    }
}
