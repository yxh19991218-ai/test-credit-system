package com.credit.system.service.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

import com.credit.system.domain.RepaymentPeriod;
import com.credit.system.domain.enums.RepaymentMethod;

/**
 * 气球贷还款计算器。
 * <p>
 * 按长期限计算月供（等额本息公式），最后 1 期偿还剩余全部本金 + 当期利息。
 * 月供基数使用 {@link BigDecimalMath#monthlyPayment} 精确计算。
 * </p>
 */
@Component
public class BalloonCalculator implements RepaymentCalculator {

    @Override
    public RepaymentMethod getMethod() {
        return RepaymentMethod.BALLOON;
    }

    @Override
    public void calculate(RepaymentPeriod period,
                          BigDecimal principal,
                          BigDecimal monthlyRate,
                          int term,
                          int periodNo,
                          BigDecimal remaining) {
        BigDecimal interest = remaining.multiply(monthlyRate).setScale(2, RoundingMode.HALF_EVEN);

        // 零利率：前 n-1 期只还利息（0），最后 1 期还全部本金
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            if (periodNo < term) {
                period.setTotalAmount(BigDecimal.ZERO.setScale(2));
                period.setPrincipal(BigDecimal.ZERO.setScale(2));
                period.setInterest(BigDecimal.ZERO.setScale(2));
            } else {
                period.setTotalAmount(remaining);
                period.setPrincipal(remaining);
                period.setInterest(BigDecimal.ZERO.setScale(2));
            }
            return;
        }

        // 模拟月供基数（按 full amortization 精确计算）
        BigDecimal monthly = BigDecimalMath.monthlyPayment(principal, monthlyRate, term);

        if (periodNo < term) {
            BigDecimal principalPart = monthly.subtract(interest);
            if (principalPart.compareTo(BigDecimal.ZERO) < 0) principalPart = BigDecimal.ZERO;
            period.setTotalAmount(monthly);
            period.setPrincipal(principalPart);
            period.setInterest(interest);
        } else {
            // 最后 1 期还剩余本金 + 利息
            period.setTotalAmount(remaining.add(interest));
            period.setPrincipal(remaining);
            period.setInterest(interest);
        }
    }
}
