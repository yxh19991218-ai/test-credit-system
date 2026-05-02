package com.credit.system.service.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

import com.credit.system.domain.RepaymentPeriod;
import com.credit.system.domain.enums.RepaymentMethod;

/**
 * 先息后本还款计算器。
 * <p>
 * 前 n-1 期只还利息，最后 1 期还全部本金 + 当期利息。
 * </p>
 */
@Component
public class InterestOnlyCalculator implements RepaymentCalculator {

    @Override
    public RepaymentMethod getMethod() {
        return RepaymentMethod.INTEREST_ONLY;
    }

    @Override
    public void calculate(RepaymentPeriod period,
                          BigDecimal principal,
                          BigDecimal monthlyRate,
                          int term,
                          int periodNo,
                          BigDecimal remaining) {
        BigDecimal interest = principal.multiply(monthlyRate).setScale(2, RoundingMode.HALF_EVEN);

        if (periodNo < term) {
            period.setTotalAmount(interest);
            period.setPrincipal(BigDecimal.ZERO);
            period.setInterest(interest);
        } else {
            period.setTotalAmount(principal.add(interest));
            period.setPrincipal(principal);
            period.setInterest(interest);
        }
    }
}
