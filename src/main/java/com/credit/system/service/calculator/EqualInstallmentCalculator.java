package com.credit.system.service.calculator;

import com.credit.system.domain.RepaymentPeriod;
import com.credit.system.domain.enums.RepaymentMethod;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 等额本息还款计算器。
 * <p>
 * 每月还款额固定 = P * r * (1+r)^n / ((1+r)^n - 1)。
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
        double p = principal.doubleValue();
        double r = monthlyRate.doubleValue();
        double n = term;

        double monthlyPayment = p * r * Math.pow(1 + r, n) / (Math.pow(1 + r, n) - 1);
        BigDecimal monthly = BigDecimal.valueOf(monthlyPayment).setScale(2, RoundingMode.HALF_UP);

        // 最后 1 期修正余差
        if (periodNo == term) {
            monthly = remaining.add(remaining.multiply(monthlyRate))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal interest = remaining.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal principalPart = monthly.subtract(interest);
        if (principalPart.compareTo(BigDecimal.ZERO) < 0) principalPart = BigDecimal.ZERO;

        period.setTotalAmount(monthly);
        period.setPrincipal(principalPart);
        period.setInterest(interest);
    }
}
