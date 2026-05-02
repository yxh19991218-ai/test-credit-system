package com.credit.system.service.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * BigDecimal 精确数学工具类。
 * <p>
 * 提供 {@link BigDecimal} 环境下高精度幂运算等函数，
 * 避免 {@code double} 转换带来的精度损失。
 * </p>
 */
public final class BigDecimalMath {

    private BigDecimalMath() {
        // 工具类，禁止实例化
    }

    /**
     * 计算 (1 + rate)^term 的精确值。
     * <p>
     * 等价于 {@code BigDecimal.ONE.add(rate).pow(term)}，
     * 语义化封装便于阅读。
     * </p>
     *
     * @param rate 利率（如 0.005 表示月利率 0.5%）
     * @param term 期数（int 范围）
     * @return (1 + rate)^term
     */
    public static BigDecimal pow1Plus(BigDecimal rate, int term) {
        return BigDecimal.ONE.add(rate).pow(term);
    }

    /**
     * 等额本息月供公式：P * r * (1+r)^n / ((1+r)^n - 1)。
     * <p>
     * 全程 {@link BigDecimal} 计算，结果保留 2 位小数，{@link RoundingMode#HALF_EVEN}。
     * </p>
     * <p>边界处理：</p>
     * <ul>
     *   <li>零利率时直接返回 {@code principal / term}（等额本金）</li>
     *   <li>零本金时返回 0</li>
     * </ul>
     *
     * @param principal   贷款本金
     * @param monthlyRate 月利率
     * @param term        总期数
     * @return 每月应还金额（保留 2 位小数）
     */
    public static BigDecimal monthlyPayment(BigDecimal principal,
                                            BigDecimal monthlyRate,
                                            int term) {
        // 零利率：直接等额本金
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(term), 2, RoundingMode.HALF_EVEN);
        }
        // 零本金
        if (principal.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(2);
        }

        BigDecimal pow = pow1Plus(monthlyRate, term);
        BigDecimal numerator = principal.multiply(monthlyRate).multiply(pow);
        BigDecimal denominator = pow.subtract(BigDecimal.ONE);
        return numerator.divide(denominator, 2, RoundingMode.HALF_EVEN);
    }
}
