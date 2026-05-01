package com.credit.system.audit;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 安全上下文工具 —— 从 SecurityContext 和 Request 中提取当前用户和 IP。
 * <p>
 * 消除各 Controller/Service 中重复的 SecurityContextHolder.getContext() 调用。
 * </p>
 */
@Component
public class SecurityUtil {

    /**
     * 获取当前登录用户名。
     * 未认证时返回 "SYSTEM"（用于事件监听器等非请求上下文）。
     */
    public String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return "SYSTEM";
    }

    /**
     * 从请求中提取客户端 IP 地址。
     */
    public String getClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For 可能包含逗号分隔的多个 IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
