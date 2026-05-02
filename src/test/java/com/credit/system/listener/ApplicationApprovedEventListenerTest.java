package com.credit.system.listener;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.credit.system.event.ApplicationApprovedEvent;
import com.credit.system.event.DomainEventWrapper;
import com.credit.system.event.ContractCreatedEvent;
import com.credit.system.service.ContractService;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApplicationApprovedEventListener 单元测试")
class ApplicationApprovedEventListenerTest {

    @Mock
    private ContractService contractService;

    @InjectMocks
    private ApplicationApprovedEventListener listener;

    @Nested
    @DisplayName("onApplicationApproved")
    class OnApplicationApproved {

        @Test
        @DisplayName("审批通过事件应触发合同创建")
        void shouldCreateContractOnApproved() {
            ApplicationApprovedEvent event = new ApplicationApprovedEvent(
                    1L, 1L, 1L,
                    new BigDecimal("100000"), 12, new BigDecimal("0.12"),
                    "admin", LocalDateTime.now(), "审批通过");
            DomainEventWrapper wrapper = new DomainEventWrapper(this, event);

            listener.onApplicationApproved(wrapper);

            verify(contractService).createContractFromApplication(1L, "SYSTEM");
        }

        @Test
        @DisplayName("非 ApplicationApprovedEvent 应忽略")
        void shouldIgnoreOtherEvents() {
            ContractCreatedEvent otherEvent = new ContractCreatedEvent(1L);
            DomainEventWrapper wrapper = new DomainEventWrapper(this, otherEvent);

            listener.onApplicationApproved(wrapper);

            verify(contractService, never()).createContractFromApplication(anyLong(), anyString());
        }
    }
}
