package com.credit.system.audit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 审计日志切面 —— 自动拦截 {@link AuditLoggable} 注解的方法并记录审计日志。
 * <p>
 * 职责：
 * <ul>
 *   <li>从 {@link SecurityUtil} 获取当前操作人</li>
 *   <li>从 Request 中提取客户端 IP</li>
 *   <li>方法执行成功后记录审计日志</li>
 *   <li>方法抛出异常时记录失败日志</li>
 * </ul>
 * </p>
 *
 * <p>
 * 使用 {@code @Order(1)} 确保在事务切面之前执行，避免事务已回滚但审计日志仍记录成功。
 * </p>
 */
@Aspect
@Component
@Order(1)
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditLogRepository auditLogRepository;
    private final SecurityUtil securityUtil;

    public AuditAspect(AuditLogRepository auditLogRepository, SecurityUtil securityUtil) {
        this.auditLogRepository = auditLogRepository;
        this.securityUtil = securityUtil;
    }

    @Around("@annotation(auditLoggable)")
    public Object audit(ProceedingJoinPoint joinPoint, AuditLoggable auditLoggable) throws Throwable {
        String operation = auditLoggable.operation();
        String entityType = auditLoggable.entityType();
        String descriptionTemplate = auditLoggable.description();

        // 提取方法参数
        Object[] args = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();

        // 尝试从参数中提取 entityId（约定：第一个 Long 参数为实体 ID）
        Long entityId = extractEntityId(args);

        // 构建描述
        String description = buildDescription(descriptionTemplate, paramNames, args);

        // 获取操作人和 IP
        String operator = securityUtil.getCurrentUsername();
        String ipAddress = getClientIp();

        // 执行目标方法
        try {
            Object result = joinPoint.proceed();

            // 记录成功日志
            AuditLog auditLog = new AuditLog(operation, operator, description,
                    entityType, entityId, ipAddress);
            auditLogRepository.save(auditLog);

            return result;
        } catch (Throwable ex) {
            // 记录失败日志
            AuditLog auditLog = new AuditLog(operation, operator, description,
                    entityType, entityId, ipAddress);
            auditLog.setSuccess(false);
            auditLog.setErrorMessage(ex.getMessage());
            auditLogRepository.save(auditLog);

            throw ex;
        }
    }

    /**
     * 从方法参数中提取实体 ID。
     * 约定：第一个 Long 类型的参数为实体 ID。
     */
    private Long extractEntityId(Object[] args) {
        if (args == null) return null;
        for (Object arg : args) {
            if (arg instanceof Long) {
                return (Long) arg;
            }
        }
        return null;
    }

    /**
     * 构建操作描述。
     * 支持简单的 {0}、{1} 占位符引用方法参数。
     */
    private String buildDescription(String template, String[] paramNames, Object[] args) {
        if (template == null || template.isEmpty()) return "";
        String result = template;
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                result = result.replace("{" + i + "}", args[i] != null ? args[i].toString() : "null");
            }
        }
        return result;
    }

    /**
     * 从当前请求中获取客户端 IP。
     */
    private String getClientIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return "unknown";
        HttpServletRequest request = attrs.getRequest();
        return securityUtil.getClientIp(request);
    }
}
