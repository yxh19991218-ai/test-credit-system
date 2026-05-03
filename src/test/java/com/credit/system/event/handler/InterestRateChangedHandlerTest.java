package com.credit.system.event.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.credit.system.event.InterestRateChangedEvent;
import com.credit.system.service.RepaymentScheduleService;

@DisplayName("InterestRateChangedHandler 单元测试")
class InterestRateChangedHandlerTest {

    private RepaymentScheduleService scheduleService;
    private InterestRateChangedHandler handler;

    @BeforeEach
    void setUp() {
        scheduleService = mock(RepaymentScheduleService.class);
        handler = new InterestRateChangedHandler(scheduleService);
    }

    @Test
    @DisplayName("应调用还款计划服务的利率变更")
    void shouldCallScheduleService() {
        InterestRateChangedEvent event = new InterestRateChangedEvent(
                1L, 1L, new BigDecimal("0.12"), new BigDecimal("0.10"),
                "MANUAL_ADJUSTMENT", "利率优惠", "ADMIN");

        handler.handle(event);

        verify(scheduleService).changeInterestRate(1L, new BigDecimal("0.10"), "利率优惠", "ADMIN");
    }

    @Test
    @DisplayName("supportsEventType 应返回 InterestRateChangedEvent")
    void shouldSupportCorrectEventType() {
        assertThat(handler.supportsEventType()).isEqualTo(InterestRateChangedEvent.class);
    }
}
