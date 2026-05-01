package com.credit.system.service.specification.impl;

import com.credit.system.domain.Customer;
import com.credit.system.service.specification.CustomerSpecification;
import com.credit.system.service.specification.SpecificationResult;
import org.springframework.stereotype.Component;

/**
 * 紧急联系人电话格式验证规约。
 * <p>
 * 紧急联系人电话必须符合中国大陆手机号格式（11 位，以 1 开头）。
 * </p>
 */
@Component
public class EmergencyContactPhoneSpecification implements CustomerSpecification<Customer> {

    @Override
    public SpecificationResult isSatisfiedBy(Customer customer) {
        if (customer.getEmergencyContactPhone() != null
                && !customer.getEmergencyContactPhone().matches("^1[3-9]\\d{9}$")) {
            return SpecificationResult.unsatisfied("紧急联系人手机号格式不正确");
        }
        return SpecificationResult.SATISFIED;
    }
}
