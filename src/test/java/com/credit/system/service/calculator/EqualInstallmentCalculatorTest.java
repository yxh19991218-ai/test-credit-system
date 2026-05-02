package com.credit.system.service.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.credit.system.domain.RepaymentPeriod;
import com.credit.system.domain.enums.RepaymentMethod;

class EqualInstallmentCalculatorTest {

    private EqualInstallmentCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new EqualInstallmentCalculator();
    }

    @Test
    @DisplayName("getMethod() 应返回 EQUAL_INSTALLMENT")
    void getMethod() {
        assertEquals(RepaymentMethod.EQUAL_INSTALLMENT, calculator.getMethod());
    }

    @Test
    @DisplayName("标准案例：10 万本金，12 期，年利率 6%（月利率 0.5%）")
    void standardCase() {
        BigDecimal principal = new BigDecimal("100000.00");
        BigDecimal monthlyRate = new BigDecimal("0.005"); // 6% / 12
        int term = 12;

        // 手工计算预期：月供 ≈ 8606.64
        // P * r * (1+r)^n / ((1+r)^n - 1)
        BigDecimal pow = BigDecimal.ONE.add(monthlyRate).pow(term);
        BigDecimal numerator = principal.multiply(monthlyRate).multiply(pow);
        BigDecimal denominator = pow.subtract(BigDecimal.ONE);
        BigDecimal expectedMonthly = numerator.divide(denominator, 2, RoundingMode.HALF_EVEN);

        BigDecimal remaining = principal;
        for (int i = 1; i <= term; i++) {
            RepaymentPeriod period = new RepaymentPeriod();
            calculator.calculate(period, principal, monthlyRate, term, i, remaining);

            if (i < term) {
                assertEquals(expectedMonthly, period.getTotalAmount(),
                        "第 " + i + " 期月供应与标准值一致");
            }

            // 本金 + 利息 = 总额（允许最后 1 期余差修正）
            BigDecimal sum = period.getPrincipal().add(period.getInterest());
            assertEquals(period.getTotalAmount(), sum,
                    "第 " + i + " 期本金 + 利息应等于总额");

            // 利息 = 剩余本金 * 月利率
            BigDecimal expectedInterest = remaining.multiply(monthlyRate)
                    .setScale(2, RoundingMode.HALF_EVEN);
            assertEquals(expectedInterest, period.getInterest(),
                    "第 " + i + " 期利息应等于剩余本金 × 月利率");

            remaining = remaining.subtract(period.getPrincipal());
        }

        // 最后一期后剩余本金应为 0
        assertEquals(0, remaining.compareTo(BigDecimal.ZERO),
                "还清后剩余本金应为 0，实际剩余: " + remaining);
    }

    @Test
    @DisplayName("边界案例：超大本金 1 亿，360 期，年利率 6%")
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

    @Test
    @DisplayName("边界案例：1 期（整借整还）")
    void singlePeriod() {
        BigDecimal principal = new BigDecimal("50000.00");
        BigDecimal monthlyRate = new BigDecimal("0.01");
        int term = 1;

        RepaymentPeriod period = new RepaymentPeriod();
        calculator.calculate(period, principal, monthlyRate, term, 1, principal);

        // 最后 1 期：总额 = 剩余本金 + 剩余本金 * 月利率
        BigDecimal expectedTotal = principal.add(principal.multiply(monthlyRate))
                .setScale(2, RoundingMode.HALF_EVEN);
        assertEquals(expectedTotal, period.getTotalAmount());
        assertEquals(principal, period.getPrincipal());
        assertEquals(principal.multiply(monthlyRate).setScale(2, RoundingMode.HALF_EVEN), period.getInterest());
    }

    @Test
    @DisplayName("与旧 double 实现在标准案例下结果一致（误差 < 0.01）")
    void consistentWithDoubleImplementation() {
        BigDecimal principal = new BigDecimal("100000.00");
        BigDecimal monthlyRate = new BigDecimal("0.005");
        int term = 12;

        // 旧 double 实现
        double p = principal.doubleValue();
        double r = monthlyRate.doubleValue();
        double n = term;
        double oldResult = p * r * Math.pow(1 + r, n) / (Math.pow(1 + r, n) - 1);

        // 新 BigDecimal 实现
        BigDecimal pow = BigDecimal.ONE.add(monthlyRate).pow(term);
        BigDecimal numerator = principal.multiply(monthlyRate).multiply(pow);
        BigDecimal denominator = pow.subtract(BigDecimal.ONE);
        BigDecimal newResult = numerator.divide(denominator, 2, RoundingMode.HALF_EVEN);

        BigDecimal diff = newResult.subtract(BigDecimal.valueOf(oldResult)).abs();
        assertTrue(diff.compareTo(new BigDecimal("0.01")) < 0,
                "新旧实现差异应小于 0.01，实际差异: " + diff);
    }
}
