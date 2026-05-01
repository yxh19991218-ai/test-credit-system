package com.credit.system.audit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要 ADMIN 角色的方法或类。
 * <p>
 * 配合 {@link SecurityAspect} 使用，在方法调用前检查当前用户是否具有 ADMIN 角色。
 * 与 Spring Security 的 {@code @PreAuthorize("hasRole('ADMIN')")} 功能等价，
 * 但提供更明确的语义和统一的错误处理。
 * </p>
 *
 * <pre>{@code
 * @RequireAdmin
 * public void batchUpdateStatus(...) { ... }
 * }</pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireAdmin {
}
