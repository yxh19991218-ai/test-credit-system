package com.credit.system.service.specification.impl;

import com.credit.system.domain.Customer;
import com.credit.system.service.specification.CustomerSpecification;
import com.credit.system.service.specification.SpecificationResult;
import com.credit.system.util.IdCardUtil;
import org.springframework.stereotype.Component;

/**
 * 年龄验证规约。
 * <p>
 * 客户年龄必须在 18-65 岁之间。
 * </p>
 */
@Component
public class AgeSpecification implements CustomerSpecification<Customer> {

    private final int minAge;
    private final int maxAge;

    public AgeSpecification() {
        this(18, 65);
    }

    public AgeSpecification(int minAge, int maxAge) {
        this.minAge = minAge;
        this.maxAge = maxAge;
    }

    @Override
    public SpecificationResult isSatisfiedBy(Customer customer) {
        int age = IdCardUtil.calculateAge(customer.getIdCard());
        if (age < minAge || age > maxAge) {
            return SpecificationResult.unsatisfied(
                    String.format("客户年龄不符合要求：当前年龄%d岁，要求%d-%d岁", age, minAge, maxAge));
        }
        return SpecificationResult.SATISFIED;
    }
}
