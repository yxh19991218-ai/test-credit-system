package com.credit.system.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventBus 单元测试")
class EventBusTest {

    @Mock
    private ApplicationEventPublisher publisher;

    @InjectMocks
    private EventBus eventBus;

    @Nested
    @DisplayName("发布事件 publish")
    class Publish {

        @Test
        @DisplayName("应发布 ApplicationApprovedEvent 包装为 DomainEventWrapper")
        void shouldPublishApplicationApprovedEvent() {
            ApplicationApprovedEvent event = new ApplicationApprovedEvent(
                    1L, 1L, 1L,
                    new BigDecimal("100000"), 12, new BigDecimal("0.12"),
                    "admin", LocalDateTime.now(), "审批通过");

            eventBus.publish(event);

            ArgumentCaptor<DomainEventWrapper> captor = ArgumentCaptor.forClass(DomainEventWrapper.class);
            verify(publisher).publishEvent(captor.capture());

            DomainEventWrapper wrapper = captor.getValue();
            assertThat(wrapper.getDomainEvent()).isInstanceOf(ApplicationApprovedEvent.class);
            ApplicationApprovedEvent captured = (ApplicationApprovedEvent) wrapper.getDomainEvent();
            assertThat(captured.getApplicationId()).isEqualTo(1L);
            assertThat(captured.getCustomerId()).isEqualTo(1L);
            assertThat(captured.getApprovedAmount()).isEqualByComparingTo(new BigDecimal("100000"));
        }

        @Test
        @DisplayName("应发布 ContractCreatedEvent 包装为 DomainEventWrapper")
        void shouldPublishContractCreatedEvent() {
            ContractCreatedEvent event = new ContractCreatedEvent(1L);

            eventBus.publish(event);

            ArgumentCaptor<DomainEventWrapper> captor = ArgumentCaptor.forClass(DomainEventWrapper.class);
            verify(publisher).publishEvent(captor.capture());

            DomainEventWrapper wrapper = captor.getValue();
            assertThat(wrapper.getDomainEvent()).isInstanceOf(ContractCreatedEvent.class);
            ContractCreatedEvent captured = (ContractCreatedEvent) wrapper.getDomainEvent();
            assertThat(captured.getContractId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("事件应包含 eventId 和 occurredOn")
        void shouldContainEventMetadata() {
            ApplicationApprovedEvent event = new ApplicationApprovedEvent(
                    1L, 1L, 1L,
                    new BigDecimal("100000"), 12, new BigDecimal("0.12"),
                    "admin", LocalDateTime.now(), "审批通过");

            assertThat(event.getEventId()).isNotNull().isNotEmpty();
            assertThat(event.getOccurredOn()).isNotNull();
        }
    }
}
