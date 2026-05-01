package com.credit.system.service.specification;

/**
 * 规约验证结果。
 *
 * @param satisfied 是否通过验证
 * @param message   失败原因（通过时为 null）
 */
public record SpecificationResult(boolean satisfied, String message) {

    /** 通过结果常量。 */
    public static final SpecificationResult SATISFIED = new SpecificationResult(true, null);

    /**
     * 创建失败结果。
     *
     * @param message 失败原因
     */
    public static SpecificationResult unsatisfied(String message) {
        return new SpecificationResult(false, message);
    }
}
