package com.credit.system.listener;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.credit.system.event.ApplicationApprovedEvent;
import com.credit.system.event.DomainEventWrapper;
import com.credit.system.event.InterestRateChangedEvent;
import com.credit.system.event.DomainEventHandler;

@DisplayName("InterestRateChangedEventListener 单元测试")
class InterestRateChangedEventListenerTest {

    private DomainEventHandler<InterestRateChangedEvent> handler;
    private InterestRateChangedEventListener listener;

    @BeforeEach
    void setUp() {
        handler = mock(DomainEventHandler.class);
        listener = new InterestRateChangedEventListener(handler);
    }

    @Nested
    @DisplayName("onInterestRateChanged")
    class OnInterestRateChanged {

        @Test
        @DisplayName("利率变更事件应路由到处理器")
        void shouldRouteToHandler() {
            InterestRateChangedEvent event = new InterestRateChangedEvent(
                    1L, 1L, new BigDecimal("0.12"), new BigDecimal("0.10"),
                    "MANUAL_ADJUSTMENT", "利率优惠", "ADMIN");
            DomainEventWrapper wrapper = new DomainEventWrapper(this, event);

            listener.onInterestRateChanged(wrapper);

            verify(handler).handle(event);
        }

        @Test
        @DisplayName("非利率变更事件应忽略")
        void shouldIgnoreOtherEvents() {
            ApplicationApprovedEvent otherEvent = new ApplicationApprovedEvent(
                    1L, 1L, 1L, null, null, null, null, null, null);
            DomainEventWrapper wrapper = new DomainEventWrapper(this, otherEvent);

            listener.onInterestRateChanged(wrapper);

            verify(handler, never()).handle(org.mockito.ArgumentMatchers.any());
        }
    }
}
