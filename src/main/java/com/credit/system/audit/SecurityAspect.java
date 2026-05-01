package com.credit.system.audit;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 安全切面 —— 拦截 {@link RequireAdmin} 注解，在方法调用前检查 ADMIN 角色。
 * <p>
 * 使用 {@code @Order(0)} 确保在 {@link AuditAspect} 之前执行，
 * 避免未授权操作被记录为审计日志。
 * </p>
 */
@Aspect
@Component
@Order(0)
public class SecurityAspect {

    private static final Logger log = LoggerFactory.getLogger(SecurityAspect.class);

    @Before("@within(requireAdmin) || @annotation(requireAdmin)")
    public void checkAdmin(RequireAdmin requireAdmin) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            log.warn("未认证用户尝试访问 ADMIN 资源");
            throw new AccessDeniedException("需要 ADMIN 角色");
        }

        boolean isAdmin = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> "ROLE_ADMIN".equals(role));

        if (!isAdmin) {
            log.warn("用户 {} 尝试访问 ADMIN 资源，当前角色: {}",
                    auth.getName(), auth.getAuthorities());
            throw new AccessDeniedException("需要 ADMIN 角色");
        }
    }
}
