package com.credit.system.service.calculator;

import com.credit.system.domain.RepaymentPeriod;
import com.credit.system.domain.enums.RepaymentMethod;

import java.math.BigDecimal;

/**
 * 还款计算策略接口。
 * <p>
 * 每种还款方式实现此接口，负责计算单期还款的本金、利息和总额。
 * 策略是无状态的，通过 {@link #getMethod()} 标识支持的还款方式。
 * </p>
 *
 * <p>调用方在循环中维护 {@code remaining}（剩余本金），
 * 每期调用 {@link #calculate(RepaymentPeriod, BigDecimal, BigDecimal, int, int, BigDecimal)}
 * 填充 {@link RepaymentPeriod} 对象。</p>
 *
 * @see EqualInstallmentCalculator
 * @see EqualPrincipalCalculator
 * @see InterestOnlyCalculator
 * @see BalloonCalculator
 * @see DueOneTimeCalculator
 */
public interface RepaymentCalculator {

    /**
     * 返回此计算器支持的还款方式。
     */
    RepaymentMethod getMethod();

    /**
     * 计算单期还款数据并填充到 {@code period} 中。
     *
     * @param period      待填充的还款期次对象
     * @param principal   贷款本金总额
     * @param monthlyRate 月利率（年利率 / 12）
     * @param term        总期数
     * @param periodNo    当前期次（从 1 开始）
     * @param remaining   当前剩余本金
     */
    void calculate(RepaymentPeriod period,
                   BigDecimal principal,
                   BigDecimal monthlyRate,
                   int term,
                   int periodNo,
                   BigDecimal remaining);
}
