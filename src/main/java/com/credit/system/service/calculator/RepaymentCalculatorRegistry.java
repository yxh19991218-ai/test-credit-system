package com.credit.system.service.calculator;

import com.credit.system.domain.enums.RepaymentMethod;
import com.credit.system.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 还款计算器注册表。
 * <p>
 * 自动收集所有 {@link RepaymentCalculator} 的 Spring Bean，
 * 按 {@link RepaymentMethod} 建立映射，提供按方式查找计算器的能力。
 * </p>
 *
 * <p>新增还款方式时，只需添加一个实现 {@link RepaymentCalculator} 的 {@code @Component} 类，
 * 无需修改注册表或调用方代码。</p>
 */
@Component
public class RepaymentCalculatorRegistry {

    private final Map<RepaymentMethod, RepaymentCalculator> calculatorMap;

    @Autowired
    public RepaymentCalculatorRegistry(List<RepaymentCalculator> calculators) {
        this.calculatorMap = new EnumMap<>(RepaymentMethod.class);
        for (RepaymentCalculator calculator : calculators) {
            this.calculatorMap.put(calculator.getMethod(), calculator);
        }
    }

    /**
     * 根据还款方式获取对应的计算器。
     *
     * @param method 还款方式
     * @return 还款计算器
     * @throws BusinessException 如果该还款方式没有对应的计算器实现
     */
    public RepaymentCalculator getCalculator(RepaymentMethod method) {
        RepaymentCalculator calculator = calculatorMap.get(method);
        if (calculator == null) {
            throw new BusinessException("不支持的还款方式: " + method);
        }
        return calculator;
    }
}
