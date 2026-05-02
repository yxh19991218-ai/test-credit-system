package com.credit.system.service.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.credit.system.domain.RepaymentPeriod;
import com.credit.system.domain.enums.RepaymentMethod;

class DueOneTimeCalculatorTest {

    private DueOneTimeCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new DueOneTimeCalculator();
    }

    @Test
    @DisplayName("getMethod() 应返回 DUE_ONE_TIME")
    void getMethod() {
        assertEquals(RepaymentMethod.DUE_ONE_TIME, calculator.getMethod());
    }

    @Test
    @DisplayName("标准案例：10 万本金，12 期，年利率 6%，前 11 期为 0，最后 1 期还本+总利息")
    void standardCase() {
        BigDecimal principal = new BigDecimal("100000.00");
        BigDecimal monthlyRate = new BigDecimal("0.005");
        int term = 12;

        // 总利息 = P * 年利率 * n / 12 = 100000 * 0.06 * 12 / 12 = 6000
        BigDecimal annualRate = monthlyRate.multiply(BigDecimal.valueOf(12));
        BigDecimal totalInterest = principal.multiply(annualRate)
                .multiply(BigDecimal.valueOf(term))
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_EVEN);

        BigDecimal remaining = principal;
        for (int i = 1; i <= term; i++) {
            RepaymentPeriod period = new RepaymentPeriod();
            calculator.calculate(period, principal, monthlyRate, term, i, remaining);

            BigDecimal sum = period.getPrincipal().add(period.getInterest());
            assertEquals(period.getTotalAmount(), sum,
                    "第 " + i + " 期本金 + 利息应等于总额");

            if (i < term) {
                assertEquals(BigDecimal.ZERO, period.getTotalAmount(),
                        "第 " + i + " 期总额应为 0");
                assertEquals(BigDecimal.ZERO, period.getPrincipal(),
                        "第 " + i + " 期本金应为 0");
                assertEquals(BigDecimal.ZERO, period.getInterest(),
                        "第 " + i + " 期利息应为 0");
            } else {
                assertEquals(totalInterest, period.getInterest(),
                        "最后 1 期利息应为总利息 " + totalInterest);
                assertEquals(principal, period.getPrincipal(),
                        "最后 1 期本金应等于全部本金");
                assertEquals(principal.add(totalInterest), period.getTotalAmount(),
                        "最后 1 期总额应等于本金 + 总利息");
            }

            remaining = remaining.subtract(period.getPrincipal());
        }

        assertEquals(0, remaining.compareTo(BigDecimal.ZERO),
                "还清后剩余本金应为 0");
    }

    @Test
    @DisplayName("边界案例：1 期")
    void singlePeriod() {
        BigDecimal principal = new BigDecimal("50000.00");
        BigDecimal monthlyRate = new BigDecimal("0.01");
        int term = 1;

        RepaymentPeriod period = new RepaymentPeriod();
        calculator.calculate(period, principal, monthlyRate, term, 1, principal);

        BigDecimal annualRate = monthlyRate.multiply(BigDecimal.valueOf(12));
        BigDecimal totalInterest = principal.multiply(annualRate)
                .multiply(BigDecimal.valueOf(term))
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_EVEN);

        assertEquals(principal, period.getPrincipal());
        assertEquals(totalInterest, period.getInterest());
        assertEquals(principal.add(totalInterest), period.getTotalAmount());
    }

    @Test
    @DisplayName("边界案例：零利率")
    void zeroInterestRate() {
        BigDecimal principal = new BigDecimal("100000.00");
        BigDecimal monthlyRate = BigDecimal.ZERO;
        int term = 12;
        BigDecimal zero = BigDecimal.ZERO.setScale(2);

        RepaymentPeriod period = new RepaymentPeriod();
        calculator.calculate(period, principal, monthlyRate, term, term, principal);

        assertEquals(principal, period.getPrincipal());
        assertEquals(0, period.getInterest().compareTo(zero));
        assertEquals(principal, period.getTotalAmount());
    }
}
