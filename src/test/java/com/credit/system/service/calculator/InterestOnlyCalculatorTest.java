package com.credit.system.service.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.credit.system.domain.RepaymentPeriod;
import com.credit.system.domain.enums.RepaymentMethod;

class InterestOnlyCalculatorTest {

    private InterestOnlyCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new InterestOnlyCalculator();
    }

    @Test
    @DisplayName("getMethod() 应返回 INTEREST_ONLY")
    void getMethod() {
        assertEquals(RepaymentMethod.INTEREST_ONLY, calculator.getMethod());
    }

    @Test
    @DisplayName("标准案例：10 万本金，12 期，年利率 6%，前 11 期只还利息，最后 1 期还本+息")
    void standardCase() {
        BigDecimal principal = new BigDecimal("100000.00");
        BigDecimal monthlyRate = new BigDecimal("0.005");
        int term = 12;

        BigDecimal expectedInterest = principal.multiply(monthlyRate).setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal remaining = principal;
        for (int i = 1; i <= term; i++) {
            RepaymentPeriod period = new RepaymentPeriod();
            calculator.calculate(period, principal, monthlyRate, term, i, remaining);

            BigDecimal sum = period.getPrincipal().add(period.getInterest());
            assertEquals(period.getTotalAmount(), sum,
                    "第 " + i + " 期本金 + 利息应等于总额");

            assertEquals(expectedInterest, period.getInterest(),
                    "第 " + i + " 期利息应为 " + expectedInterest);

            if (i < term) {
                assertEquals(BigDecimal.ZERO, period.getPrincipal(),
                        "第 " + i + " 期本金应为 0");
                assertEquals(expectedInterest, period.getTotalAmount(),
                        "第 " + i + " 期总额应等于利息");
            } else {
                assertEquals(principal, period.getPrincipal(),
                        "最后 1 期本金应等于全部本金");
                assertEquals(principal.add(expectedInterest), period.getTotalAmount(),
                        "最后 1 期总额应等于本金 + 利息");
            }

            remaining = remaining.subtract(period.getPrincipal());
        }

        assertEquals(0, remaining.compareTo(BigDecimal.ZERO),
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

        BigDecimal expectedInterest = principal.multiply(monthlyRate).setScale(2, RoundingMode.HALF_EVEN);
        assertEquals(principal, period.getPrincipal());
        assertEquals(0, period.getInterest().compareTo(expectedInterest));
        assertEquals(principal.add(expectedInterest), period.getTotalAmount());
    }
}
