package com.credit.system.service.calculator;

import com.credit.system.domain.RepaymentPeriod;
import com.credit.system.domain.enums.RepaymentMethod;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 气球贷还款计算器。
 * <p>
 * 按长期限计算月供（等额本息公式），最后 1 期偿还剩余全部本金 + 当期利息。
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
        BigDecimal interest = remaining.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);

        // 模拟月供基数（按 full amortization 计算）
        double p = principal.doubleValue();
        double r = monthlyRate.doubleValue();
        double n = term;
        double monthlyPayment = p * r * Math.pow(1 + r, n) / (Math.pow(1 + r, n) - 1);
        BigDecimal monthly = BigDecimal.valueOf(monthlyPayment).setScale(2, RoundingMode.HALF_UP);

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
