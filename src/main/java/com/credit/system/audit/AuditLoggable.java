package com.credit.system.audit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要记录审计日志的方法。
 * <p>
 * 配合 {@link AuditAspect} 使用，自动记录方法调用的审计信息。
 * </p>
 *
 * <pre>{@code
 * @AuditLoggable(operation = "CREATE_CUSTOMER", entityType = "Customer")
 * public Customer createCustomer(Customer customer) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLoggable {

    /** 操作类型，如 CREATE_CUSTOMER、REVIEW_APPLICATION */
    String operation();

    /** 实体类型，如 Customer、LoanApplication */
    String entityType() default "";

    /** 操作描述模板，支持 SpEL 表达式引用方法参数，如 "创建客户 #customer.name" */
    String description() default "";
}
