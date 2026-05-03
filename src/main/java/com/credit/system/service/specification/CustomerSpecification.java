package com.credit.system.service.specification;

import com.credit.system.domain.Customer;

/**
 * 客户规约接口。
 * <p>
 * 每个规约封装一条独立的客户验证规则，支持通过 {@link #and(CustomerSpecification)}
 * 组合多个规约形成验证链。
 * </p>
 *
 * @param <T> 规约适用的客户类型
 */
public interface CustomerSpecification<T extends Customer> {

    /**
     * 验证客户是否满足规约条件。
     *
     * @param customer 待验证的客户
     * @return 验证结果，包含是否通过及失败原因
     */
    SpecificationResult isSatisfiedBy(T customer);

    /**
     * 组合当前规约与另一个规约（AND 逻辑）。
     */
    default CustomerSpecification<T> and(CustomerSpecification<T> other) {
        return customer -> {
            SpecificationResult thisResult = this.isSatisfiedBy(customer);
            if (!thisResult.satisfied()) {
                return thisResult;
            }
            return other.isSatisfiedBy(customer);
        };
    }
}
