package com.credit.system.service.specification.impl;

import com.credit.system.domain.Customer;
import com.credit.system.repository.CustomerRepository;
import com.credit.system.service.specification.CustomerSpecification;
import com.credit.system.service.specification.SpecificationResult;
import org.springframework.stereotype.Component;

/**
 * 黑名单检查规约。
 * <p>
 * 检查客户的身份证号和手机号是否在黑名单中。
 * </p>
 */
@Component
public class BlacklistSpecification implements CustomerSpecification<Customer> {

    private final CustomerRepository customerRepository;

    public BlacklistSpecification(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public SpecificationResult isSatisfiedBy(Customer customer) {
        if (customerRepository.existsInBlacklistByIdCard(customer.getIdCard())) {
            return SpecificationResult.unsatisfied("客户身份证号在黑名单中");
        }
        if (customerRepository.existsInBlacklistByPhone(customer.getPhone())) {
            return SpecificationResult.unsatisfied("客户手机号在黑名单中");
        }
        return SpecificationResult.SATISFIED;
    }
}
