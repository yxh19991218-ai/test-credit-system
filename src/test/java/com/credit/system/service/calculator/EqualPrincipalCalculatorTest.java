package com.credit.system.service.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.credit.system.domain.RepaymentPeriod;
import com.credit.system.domain.enums.RepaymentMethod;

class EqualPrincipalCalculatorTest {

    private EqualPrincipalCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new EqualPrincipalCalculator();
    }

    @Test
    @DisplayName("getMethod() 应返回 EQUAL_PRINCIPAL")
    void getMethod() {
        assertEquals(RepaymentMethod.EQUAL_PRINCIPAL, calculator.getMethod());
    }

    @Test
    @DisplayName("标准案例：10 万本金，12 期，年利率 6%")
    void standardCase() {
        BigDecimal principal = new BigDecimal("100000.00");
        BigDecimal monthlyRate = new BigDecimal("0.005");
        int term = 12;

        BigDecimal expectedPrincipalPerPeriod = principal.divide(BigDecimal.valueOf(term), 2, RoundingMode.HALF_EVEN);

        BigDecimal remaining = principal;
        for (int i = 1; i <= term; i++) {
            RepaymentPeriod period = new RepaymentPeriod();
            calculator.calculate(period, principal, monthlyRate, term, i, remaining);

            BigDecimal sum = period.getPrincipal().add(period.getInterest());
            assertEquals(period.getTotalAmount(), sum,
                    "第 " + i + " 期本金 + 利息应等于总额");

            BigDecimal expectedInterest = remaining.multiply(monthlyRate)
                    .setScale(2, RoundingMode.HALF_EVEN);
            assertEquals(expectedInterest, period.getInterest(),
                    "第 " + i + " 期利息应等于剩余本金 × 月利率");

            if (i < term) {
                assertEquals(expectedPrincipalPerPeriod, period.getPrincipal(),
                        "第 " + i + " 期本金应为 " + expectedPrincipalPerPeriod);
            }

            remaining = remaining.subtract(period.getPrincipal());
        }

        assertEquals(0, remaining.compareTo(BigDecimal.ZERO),
                "还清后剩余本金应为 0，实际剩余: " + remaining);
    }

    @Test
    @DisplayName("边界案例：超大本金 1 亿，360 期")
    void largePrincipalLongTerm() {
        BigDecimal principal = new BigDecimal("100000000.00");
        BigDecimal monthlyRate = new BigDecimal("0.005");
        int term = 360;

        BigDecimal remaining = principal;
        for (int i = 1; i <= term; i++) {
            RepaymentPeriod period = new RepaymentPeriod();
            calculator.calculate(period, principal, monthlyRate, term, i, remaining);

            BigDecimal sum = period.getPrincipal().add(period.getInterest());
            assertEquals(period.getTotalAmount(), sum,
                    "第 " + i + " 期本金 + 利息应等于总额");

            remaining = remaining.subtract(period.getPrincipal());
        }

        assertEquals(0, remaining.compareTo(BigDecimal.ZERO),
                "还清后剩余本金应为 0，实际剩余: " + remaining);
    }

    @Test
    @DisplayName("边界案例：零利率")
    void zeroInterestRate() {
        BigDecimal principal = new BigDecimal("100000.00");
        BigDecimal monthlyRate = BigDecimal.ZERO;
        int term = 12;

        BigDecimal expectedPrincipalPerPeriod = principal.divide(BigDecimal.valueOf(term), 2, RoundingMode.HALF_EVEN);
        BigDecimal zero = BigDecimal.ZERO.setScale(2);

        BigDecimal remaining = principal;
        for (int i = 1; i <= term; i++) {
            RepaymentPeriod period = new RepaymentPeriod();
            calculator.calculate(period, principal, monthlyRate, term, i, remaining);

            assertEquals(0, period.getInterest().compareTo(zero),
                    "第 " + i + " 期利息应为 0");

            if (i < term) {
                assertEquals(expectedPrincipalPerPeriod, period.getPrincipal(),
                        "第 " + i + " 期本金应为 " + expectedPrincipalPerPeriod);
            }

            remaining = remaining.subtract(period.getPrincipal());
        }

        assertEquals(0, remaining.compareTo(BigDecimal.ZERO.setScale(2)),
                "还清后剩余本金应为 0");
    }
}
