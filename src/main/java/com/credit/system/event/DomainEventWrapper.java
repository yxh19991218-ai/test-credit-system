package com.credit.system.event;

import org.springframework.context.ApplicationEvent;

/**
 * Spring 事件包装器。
 * <p>
 * 将 {@link DomainEvent} 包装为 Spring 的 {@link ApplicationEvent}，
 * 使监听器仍可使用 {@link org.springframework.transaction.event.TransactionalEventListener}。
 * 此包装器对领域层透明，发布者只需通过 {@link EventBus#publish(DomainEvent)} 发布。
 * </p>
 */
public class DomainEventWrapper extends ApplicationEvent {

    private final DomainEvent domainEvent;

    public DomainEventWrapper(Object source, DomainEvent domainEvent) {
        super(source);
        this.domainEvent = domainEvent;
    }

    /** 获取被包装的领域事件。 */
    public DomainEvent getDomainEvent() {
        return domainEvent;
    }
}
