package com.credit.system.service.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

import com.credit.system.domain.RepaymentPeriod;
import com.credit.system.domain.enums.RepaymentMethod;

/**
 * 等额本金还款计算器。
 * <p>
 * 每期偿还固定本金 = P / n，利息逐期递减。
 * 最后 1 期修正因四舍五入产生的余差。
 * </p>
 */
@Component
public class EqualPrincipalCalculator implements RepaymentCalculator {

    @Override
    public RepaymentMethod getMethod() {
        return RepaymentMethod.EQUAL_PRINCIPAL;
    }

    @Override
    public void calculate(RepaymentPeriod period,
                          BigDecimal principal,
                          BigDecimal monthlyRate,
                          int term,
                          int periodNo,
                          BigDecimal remaining) {
        BigDecimal principalPerPeriod = principal.divide(BigDecimal.valueOf(term), 2, RoundingMode.HALF_EVEN);

        // 最后 1 期修正
        BigDecimal actualPrincipal = principalPerPeriod;
        if (periodNo == term) {
            BigDecimal paid = principalPerPeriod.multiply(BigDecimal.valueOf(term - 1));
            actualPrincipal = principal.subtract(paid);
            if (actualPrincipal.compareTo(BigDecimal.ZERO) < 0) actualPrincipal = BigDecimal.ZERO;
        }

        BigDecimal interest = remaining.multiply(monthlyRate).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal total = actualPrincipal.add(interest);

        period.setTotalAmount(total);
        period.setPrincipal(actualPrincipal);
        period.setInterest(interest);
    }
}
