package com.credit.system.service.calculator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.credit.system.domain.enums.RepaymentMethod;
import com.credit.system.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
@DisplayName("RepaymentCalculatorRegistry 单元测试")
class RepaymentCalculatorRegistryTest {

    private RepaymentCalculatorRegistry registry;

    @BeforeEach
    void setUp() {
        // 模拟 Spring 自动注入所有 RepaymentCalculator Bean
        List<RepaymentCalculator> calculators = List.of(
                new EqualInstallmentCalculator(),
                new EqualPrincipalCalculator(),
                new BalloonCalculator(),
                new DueOneTimeCalculator(),
                new InterestOnlyCalculator()
        );
        registry = new RepaymentCalculatorRegistry(calculators);
    }

    @Nested
    @DisplayName("getCalculator")
    class GetCalculator {

        @Test
        @DisplayName("应返回等额本息计算器")
        void shouldReturnEqualInstallmentCalculator() {
            RepaymentCalculator calculator = registry.getCalculator(RepaymentMethod.EQUAL_INSTALLMENT);
            assertThat(calculator).isInstanceOf(EqualInstallmentCalculator.class);
            assertThat(calculator.getMethod()).isEqualTo(RepaymentMethod.EQUAL_INSTALLMENT);
        }

        @Test
        @DisplayName("应返回等额本金计算器")
        void shouldReturnEqualPrincipalCalculator() {
            RepaymentCalculator calculator = registry.getCalculator(RepaymentMethod.EQUAL_PRINCIPAL);
            assertThat(calculator).isInstanceOf(EqualPrincipalCalculator.class);
            assertThat(calculator.getMethod()).isEqualTo(RepaymentMethod.EQUAL_PRINCIPAL);
        }

        @Test
        @DisplayName("应返回气球贷计算器")
        void shouldReturnBalloonCalculator() {
            RepaymentCalculator calculator = registry.getCalculator(RepaymentMethod.BALLOON);
            assertThat(calculator).isInstanceOf(BalloonCalculator.class);
            assertThat(calculator.getMethod()).isEqualTo(RepaymentMethod.BALLOON);
        }

        @Test
        @DisplayName("应返回到期一次性还本付息计算器")
        void shouldReturnDueOneTimeCalculator() {
            RepaymentCalculator calculator = registry.getCalculator(RepaymentMethod.DUE_ONE_TIME);
            assertThat(calculator).isInstanceOf(DueOneTimeCalculator.class);
            assertThat(calculator.getMethod()).isEqualTo(RepaymentMethod.DUE_ONE_TIME);
        }

        @Test
        @DisplayName("应返回先息后本计算器")
        void shouldReturnInterestOnlyCalculator() {
            RepaymentCalculator calculator = registry.getCalculator(RepaymentMethod.INTEREST_ONLY);
            assertThat(calculator).isInstanceOf(InterestOnlyCalculator.class);
            assertThat(calculator.getMethod()).isEqualTo(RepaymentMethod.INTEREST_ONLY);
        }

        @Test
        @DisplayName("不支持的还款方式应抛出异常")
        void shouldThrowForUnsupportedMethod() {
            // 使用 null 模拟不支持的还款方式
            assertThatThrownBy(() -> registry.getCalculator(null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("不支持的还款方式");
        }
    }
}
