package com.credit.system.listener;

import com.credit.system.event.DomainEventWrapper;
import com.credit.system.event.InterestRateChangedEvent;
import com.credit.system.event.DomainEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class InterestRateChangedEventListener {

    private static final Logger log = LoggerFactory.getLogger(InterestRateChangedEventListener.class);

    private final DomainEventHandler<InterestRateChangedEvent> handler;

    public InterestRateChangedEventListener(DomainEventHandler<InterestRateChangedEvent> handler) {
        this.handler = handler;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInterestRateChanged(DomainEventWrapper wrapper) {
        if (wrapper.getDomainEvent() instanceof InterestRateChangedEvent event) {
            log.debug("路由利率变更事件到处理器，eventId={}", event.getEventId());
            handler.handle(event);
        }
    }
}
