package com.credit.system.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 领域事件总线。
 * <p>
 * 封装 Spring 的 {@link ApplicationEventPublisher}，提供统一的领域事件发布入口。
 * 发布者只需调用 {@link #publish(DomainEvent)}，无需直接操作 Spring 事件 API。
 * </p>
 */
@Component
public class EventBus {

    private static final Logger log = LoggerFactory.getLogger(EventBus.class);

    private final ApplicationEventPublisher publisher;

    public EventBus(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    /**
     * 发布领域事件。
     * <p>
     * 将 {@link DomainEvent} 包装为 Spring 的 {@link DomainEventWrapper} 后发布，
     * 使监听器仍可使用 {@link org.springframework.transaction.event.TransactionalEventListener}。
     * </p>
     *
     * @param event 领域事件
     */
    public void publish(DomainEvent event) {
        log.debug("发布领域事件: type={}, eventId={}", event.getClass().getSimpleName(), event.getEventId());
        publisher.publishEvent(new DomainEventWrapper(this, event));
    }
}
