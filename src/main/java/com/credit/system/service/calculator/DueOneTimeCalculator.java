package com.credit.system.service.calculator;

import com.credit.system.domain.RepaymentPeriod;
import com.credit.system.domain.enums.RepaymentMethod;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 到期一次性还本付息计算器。
 * <p>
 * 前 n-1 期金额为 0，最后 1 期偿还全部本金 + 总利息。
 * 总利息 = P * 年利率 * n / 12。
 * </p>
 */
@Component
public class DueOneTimeCalculator implements RepaymentCalculator {

    @Override
    public RepaymentMethod getMethod() {
        return RepaymentMethod.DUE_ONE_TIME;
    }

    @Override
    public void calculate(RepaymentPeriod period,
                          BigDecimal principal,
                          BigDecimal monthlyRate,
                          int term,
                          int periodNo,
                          BigDecimal remaining) {
        if (periodNo < term) {
            period.setTotalAmount(BigDecimal.ZERO);
            period.setPrincipal(BigDecimal.ZERO);
            period.setInterest(BigDecimal.ZERO);
        } else {
            // 注意：此处 annualRate 通过 monthlyRate * 12 还原
            BigDecimal annualRate = monthlyRate.multiply(BigDecimal.valueOf(12));
            BigDecimal totalInterest = principal.multiply(annualRate)
                    .multiply(BigDecimal.valueOf(term))
                    .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
            period.setTotalAmount(principal.add(totalInterest));
            period.setPrincipal(principal);
            period.setInterest(totalInterest);
        }
    }
}
