package com.credit.system.service.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

import com.credit.system.domain.RepaymentPeriod;
import com.credit.system.domain.enums.RepaymentMethod;

/**
 * 等额本息还款计算器。
 * <p>
 * 每月还款额固定 = P * r * (1+r)^n / ((1+r)^n - 1)。
 * 全程 {@link BigDecimal} 精确计算，使用 {@link BigDecimalMath#monthlyPayment}。
 * 最后 1 期修正余差。
 * </p>
 */
@Component
public class EqualInstallmentCalculator implements RepaymentCalculator {

    @Override
    public RepaymentMethod getMethod() {
        return RepaymentMethod.EQUAL_INSTALLMENT;
    }

    @Override
    public void calculate(RepaymentPeriod period,
                          BigDecimal principal,
                          BigDecimal monthlyRate,
                          int term,
                          int periodNo,
                          BigDecimal remaining) {
        // 精确计算月供（全程 BigDecimal，无 double 转换）
        BigDecimal monthly = BigDecimalMath.monthlyPayment(principal, monthlyRate, term);

        // 最后 1 期修正余差
        if (periodNo == term) {
            monthly = remaining.add(remaining.multiply(monthlyRate))
                    .setScale(2, RoundingMode.HALF_EVEN);
        }

        BigDecimal interest = remaining.multiply(monthlyRate).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal principalPart = monthly.subtract(interest);
        if (principalPart.compareTo(BigDecimal.ZERO) < 0) principalPart = BigDecimal.ZERO;

        period.setTotalAmount(monthly);
        period.setPrincipal(principalPart);
        period.setInterest(interest);
    }
}
