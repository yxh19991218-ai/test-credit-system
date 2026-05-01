package com.credit.system.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 领域事件基类。
 * <p>
 * 所有领域事件继承此类，提供事件 ID、时间戳等公共元数据。
 * 不再继承 Spring 的 {@link org.springframework.context.ApplicationEvent}，
 * 通过 {@link EventBus} 统一发布。
 * </p>
 */
public abstract class DomainEvent {

    private final String eventId;
    private final LocalDateTime occurredOn;

    protected DomainEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = LocalDateTime.now();
    }

    /** 事件唯一标识。 */
    public String getEventId() {
        return eventId;
    }

    /** 事件发生时间。 */
    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }
}
