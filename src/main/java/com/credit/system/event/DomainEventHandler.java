package com.credit.system.event;

/**
 * 领域事件处理器接口。
 * <p>
 * 每个领域事件对应一个处理器实现，负责处理事件触发的业务逻辑。
 * 监听器层只做路由，将事件委托给对应的处理器。
 * </p>
 *
 * @param <T> 处理器能处理的领域事件类型
 */
public interface DomainEventHandler<T extends DomainEvent> {

    /**
     * 处理领域事件。
     *
     * @param event 待处理的事件
     */
    void handle(T event);

    /**
     * 返回此处理器能处理的事件类型。
     */
    Class<T> supportsEventType();
}
