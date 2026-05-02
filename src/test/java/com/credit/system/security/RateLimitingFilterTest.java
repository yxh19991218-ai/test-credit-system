package com.credit.system.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.ServletException;
import java.io.IOException;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitingFilter 单元测试")
class RateLimitingFilterTest {

    private RateLimitingFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new RateLimitingFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
    }

    @Nested
    @DisplayName("doFilter")
    class DoFilter {

        @Test
        @DisplayName("白名单路径应跳过限流")
        void shouldSkipWhitelistPaths() throws ServletException, IOException {
            request.setRequestURI("/api/auth/login");
            request.setRemoteAddr("192.168.1.1");

            filter.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isNotEqualTo(429);
        }

        @Test
        @DisplayName("健康检查路径应跳过限流")
        void shouldSkipHealthCheckPath() throws ServletException, IOException {
            request.setRequestURI("/actuator/health");
            request.setRemoteAddr("192.168.1.1");

            filter.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isNotEqualTo(429);
        }

        @Test
        @DisplayName("API 文档路径应跳过限流")
        void shouldSkipApiDocsPath() throws ServletException, IOException {
            request.setRequestURI("/v3/api-docs");
            request.setRemoteAddr("192.168.1.1");

            filter.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isNotEqualTo(429);
        }

        @Test
        @DisplayName("正常请求应通过")
        void shouldAllowNormalRequest() throws ServletException, IOException {
            request.setRequestURI("/api/customers");
            request.setRemoteAddr("192.168.1.1");

            filter.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isNotEqualTo(429);
        }

        @Test
        @DisplayName("超过限流应返回 429")
        void shouldReturn429WhenRateExceeded() throws ServletException, IOException {
            request.setRequestURI("/api/customers");
            request.setRemoteAddr("192.168.1.100");

            // 发送 101 次请求（超过 100 次/分钟的限制）
            for (int i = 0; i < 101; i++) {
                response = new MockHttpServletResponse();
                filterChain = new MockFilterChain();
                filter.doFilter(request, response, filterChain);
            }

            assertThat(response.getStatus()).isEqualTo(429);
            assertThat(response.getContentAsString()).contains("请求过于频繁");
        }

        @Test
        @DisplayName("不同 IP 应独立计数")
        void shouldCountSeparatelyForDifferentIps() throws ServletException, IOException {
            request.setRequestURI("/api/customers");

            // IP1 发送 101 次请求
            request.setRemoteAddr("192.168.1.1");
            for (int i = 0; i < 101; i++) {
                response = new MockHttpServletResponse();
                filterChain = new MockFilterChain();
                filter.doFilter(request, response, filterChain);
            }
            assertThat(response.getStatus()).isEqualTo(429);

            // IP2 发送请求应正常
            request.setRemoteAddr("192.168.1.2");
            response = new MockHttpServletResponse();
            filterChain = new MockFilterChain();
            filter.doFilter(request, response, filterChain);
            assertThat(response.getStatus()).isNotEqualTo(429);
        }

        @Test
        @DisplayName("X-Forwarded-For 头应正确提取客户端 IP")
        void shouldUseXForwardedForHeader() throws ServletException, IOException {
            request.setRequestURI("/api/customers");
            request.addHeader("X-Forwarded-For", "10.0.0.1, 10.0.0.2");
            request.setRemoteAddr("192.168.1.1");

            // 发送 101 次请求
            for (int i = 0; i < 101; i++) {
                response = new MockHttpServletResponse();
                filterChain = new MockFilterChain();
                filter.doFilter(request, response, filterChain);
            }

            assertThat(response.getStatus()).isEqualTo(429);
        }
    }
}
