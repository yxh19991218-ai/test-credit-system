package com.credit.system.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * API 限流过滤器 —— 基于 IP 的简单滑动窗口限流。
 * <p>
 * 默认限制：每分钟 100 次请求。
 * 超过限制返回 429 Too Many Requests。
 * </p>
 */
@Component
@Order(1)
public class RateLimitingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    /** 每分钟最大请求数 */
    private static final int MAX_REQUESTS_PER_MINUTE = 100;

    /** 窗口大小（毫秒） */
    private static final long WINDOW_SIZE_MS = 60_000;

    /** IP -> 请求计数 */
    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain filterChain) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        // 白名单路径不限制
        String path = request.getRequestURI();
        if (path.contains("/api/auth/") || path.contains("/actuator/health")
                || path.contains("/v3/api-docs") || path.contains("/swagger-ui")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        long now = System.currentTimeMillis();

        WindowCounter counter = counters.compute(clientIp, (key, existing) -> {
            if (existing == null || now - existing.windowStart > WINDOW_SIZE_MS) {
                return new WindowCounter(now, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });

        if (counter.count.get() > MAX_REQUESTS_PER_MINUTE) {
            log.warn("API 限流触发 - IP: {}, 请求数: {}", clientIp, counter.count.get());
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private record WindowCounter(long windowStart, AtomicInteger count) {}
}
