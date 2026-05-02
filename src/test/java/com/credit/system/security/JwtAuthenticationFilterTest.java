package com.credit.system.security;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;

@DisplayName("JwtAuthenticationFilter 单元测试")
class JwtAuthenticationFilterTest {

    private JwtTokenProvider tokenProvider;
    private JwtAuthenticationFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;

    private static final String TEST_USERNAME = "admin";
    private static final String TEST_ROLE = "ADMIN";

    @BeforeEach
    void setUp() {
        // JwtTokenProvider 在 Java 25 上无法被 Mockito mock，使用真实实例
        // 使用一个测试用的 base64 密钥（至少 256 位 = 32 字节）
        String testSecret = "dGVzdC1zZWNyZXQta2V5LWZvci1qd3QtdG9rZW4tcHJvdmlkZXItdGVzdA==";
        tokenProvider = new JwtTokenProvider(testSecret, 3600000, 604800000);
        filter = new JwtAuthenticationFilter(tokenProvider);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = Mockito.mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("doFilterInternal")
    class DoFilterInternal {

        @Test
        @DisplayName("有效令牌应设置认证信息")
        void shouldSetAuthenticationForValidToken() throws ServletException, IOException {
            // 生成一个真实的 JWT 令牌
            String validToken = tokenProvider.generateAccessToken(TEST_USERNAME, TEST_ROLE);
            request.addHeader("Authorization", "Bearer " + validToken);

            filter.doFilterInternal(request, response, filterChain);

            var authentication = SecurityContextHolder.getContext().getAuthentication();
            assert authentication != null;
            assert authentication.getName().equals(TEST_USERNAME);
            assert authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("无令牌时应继续过滤器链")
        void shouldContinueChainWhenNoToken() throws ServletException, IOException {
            filter.doFilterInternal(request, response, filterChain);

            var authentication = SecurityContextHolder.getContext().getAuthentication();
            assert authentication == null;
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("无效令牌应继续过滤器链但不设置认证")
        void shouldContinueChainForInvalidToken() throws ServletException, IOException {
            request.addHeader("Authorization", "Bearer invalid-token");

            filter.doFilterInternal(request, response, filterChain);

            var authentication = SecurityContextHolder.getContext().getAuthentication();
            assert authentication == null;
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("非 Bearer 前缀的令牌应忽略")
        void shouldIgnoreNonBearerToken() throws ServletException, IOException {
            request.addHeader("Authorization", "Basic some-base64-credentials");

            filter.doFilterInternal(request, response, filterChain);

            var authentication = SecurityContextHolder.getContext().getAuthentication();
            assert authentication == null;
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("空 Authorization 头应忽略")
        void shouldIgnoreEmptyAuthHeader() throws ServletException, IOException {
            request.addHeader("Authorization", "");

            filter.doFilterInternal(request, response, filterChain);

            var authentication = SecurityContextHolder.getContext().getAuthentication();
            assert authentication == null;
            verify(filterChain).doFilter(request, response);
        }
    }
}
