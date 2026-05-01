package com.credit.system;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.credit.system.event.EventBus;
import com.credit.system.service.CustomerService;
import com.credit.system.service.LoanApplicationService;
import com.credit.system.service.RepaymentScheduleService;

/**
 * 集成契约测试：验证 Spring 上下文加载正常，核心 Bean 可被正确注入。
 * <p>
 * 使用 {@link IntegrationTest} 自定义注解，仅加载所需配置，
 * 不启动 Web 环境，测试速度更快。
 * </p>
 */
@IntegrationTest
@DisplayName("核心集成契约测试")
class CreditSystemIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private LoanApplicationService applicationService;

    @Autowired
    private RepaymentScheduleService scheduleService;

    @Autowired
    private EventBus eventBus;

    @Test
    @DisplayName("Spring 上下文应成功加载")
    void contextShouldLoad() {
        assertThat(applicationContext).isNotNull();
    }

    @Test
    @DisplayName("核心 Service Bean 应全部注入成功")
    void coreServicesShouldBeAvailable() {
        assertThat(customerService).isNotNull();
        assertThat(applicationService).isNotNull();
        assertThat(scheduleService).isNotNull();
    }

    @Test
    @DisplayName("EventBus 应注入成功")
    void eventBusShouldBeAvailable() {
        assertThat(eventBus).isNotNull();
    }
}
