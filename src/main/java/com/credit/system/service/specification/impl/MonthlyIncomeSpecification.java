package com.credit.system.service.specification.impl;

import com.credit.system.domain.Customer;
import com.credit.system.service.specification.CustomerSpecification;
import com.credit.system.service.specification.SpecificationResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 月收入验证规约。
 * <p>
 * 月收入不能为负数。
 * </p>
 */
@Component
public class MonthlyIncomeSpecification implements CustomerSpecification<Customer> {

    @Override
    public SpecificationResult isSatisfiedBy(Customer customer) {
        if (customer.getMonthlyIncome() != null
                && customer.getMonthlyIncome().compareTo(BigDecimal.ZERO) < 0) {
            return SpecificationResult.unsatisfied("月收入不能为负数");
        }
        return SpecificationResult.SATISFIED;
    }
}
