package com.credit.system.service.specification.impl;

import com.credit.system.domain.Customer;
import com.credit.system.repository.CustomerRepository;
import com.credit.system.service.specification.CustomerSpecification;
import com.credit.system.service.specification.SpecificationResult;
import org.springframework.stereotype.Component;

/**
 * 手机号唯一性验证规约。
 * <p>
 * 新客户的手机号不能与已有客户重复。
 * </p>
 */
@Component
public class PhoneUniquenessSpecification implements CustomerSpecification<Customer> {

    private final CustomerRepository customerRepository;

    public PhoneUniquenessSpecification(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public SpecificationResult isSatisfiedBy(Customer customer) {
        if (customerRepository.existsByPhone(customer.getPhone())) {
            return SpecificationResult.unsatisfied("手机号已存在: " + customer.getPhone());
        }
        return SpecificationResult.SATISFIED;
    }
}
