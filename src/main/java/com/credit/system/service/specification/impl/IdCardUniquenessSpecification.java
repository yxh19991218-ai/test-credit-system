package com.credit.system.service.specification.impl;

import com.credit.system.domain.Customer;
import com.credit.system.repository.CustomerRepository;
import com.credit.system.service.specification.CustomerSpecification;
import com.credit.system.service.specification.SpecificationResult;
import org.springframework.stereotype.Component;

/**
 * 身份证号唯一性验证规约。
 * <p>
 * 新客户的身份证号不能与已有客户重复。
 * </p>
 */
@Component
public class IdCardUniquenessSpecification implements CustomerSpecification<Customer> {

    private final CustomerRepository customerRepository;

    public IdCardUniquenessSpecification(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public SpecificationResult isSatisfiedBy(Customer customer) {
        if (customerRepository.existsByIdCard(customer.getIdCard())) {
            return SpecificationResult.unsatisfied("身份证号已存在: " + customer.getIdCard());
        }
        return SpecificationResult.SATISFIED;
    }
}
