package com.credit.system.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.credit.system.dto.ApiResponse;
import com.credit.system.security.JwtTokenProvider;

@DisplayName("AuthController 单元测试")
class AuthControllerTest {

    private JwtTokenProvider tokenProvider;
    private AuthenticationManager authenticationManager;
    private AuthController authController;

    private static final String TEST_USERNAME = "admin";
    private static final String TEST_PASSWORD = "password123";
    private static final String TEST_ACCESS_TOKEN = "test-access-token";
    private static final String TEST_REFRESH_TOKEN = "test-refresh-token";

    @BeforeEach
    void setUp() {
        // JwtTokenProvider 在 Java 25 上无法被 Mockito mock，使用真实实例
        String testSecret = "dGVzdC1zZWNyZXQta2V5LWZvci1qd3QtdG9rZW4tcHJvdmlkZXItdGVzdA==";
        tokenProvider = new JwtTokenProvider(testSecret, 3600000, 604800000);
        authenticationManager = authentication -> {
            if (authentication.getCredentials().equals(TEST_PASSWORD)) {
                return new UsernamePasswordAuthenticationToken(
                        authentication.getPrincipal(), authentication.getCredentials(),
                        java.util.List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
            }
            throw new BadCredentialsException("用户名或密码错误");
        };
        authController = new AuthController(tokenProvider, authenticationManager);
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        private AuthController.LoginRequest loginRequest;

        @BeforeEach
        void setUp() {
            loginRequest = new AuthController.LoginRequest(TEST_USERNAME, TEST_PASSWORD);
        }

        @Test
        @DisplayName("应成功登录并返回令牌")
        void shouldLoginSuccessfully() {
            ResponseEntity<ApiResponse<Map<String, String>>> response =
                    authController.login(loginRequest);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody().getCode()).isEqualTo(200);
            assertThat(response.getBody().getData())
                    .containsEntry("tokenType", "Bearer")
                    .containsEntry("username", TEST_USERNAME)
                    .containsEntry("role", "ADMIN");
            assertThat(response.getBody().getData().get("accessToken")).isNotNull().isNotEmpty();
            assertThat(response.getBody().getData().get("refreshToken")).isNotNull().isNotEmpty();
        }

        @Test
        @DisplayName("USER 角色登录应返回正确角色")
        void shouldReturnUserRole() {
            authenticationManager = authentication -> {
                if (authentication.getCredentials().equals(TEST_PASSWORD)) {
                    return new UsernamePasswordAuthenticationToken(
                            authentication.getPrincipal(), authentication.getCredentials(),
                            java.util.List.of(new SimpleGrantedAuthority("ROLE_USER")));
                }
                throw new BadCredentialsException("用户名或密码错误");
            };
            authController = new AuthController(tokenProvider, authenticationManager);

            ResponseEntity<ApiResponse<Map<String, String>>> response =
                    authController.login(new AuthController.LoginRequest("zhangsan", TEST_PASSWORD));

            assertThat(response.getBody().getData())
                    .containsEntry("role", "USER");
        }

        @Test
        @DisplayName("密码错误应抛出 BadCredentialsException")
        void shouldThrowWhenBadCredentials() {
            authenticationManager = authentication -> {
                throw new BadCredentialsException("用户名或密码错误");
            };
            authController = new AuthController(tokenProvider, authenticationManager);

            assertThatThrownBy(() -> authController.login(loginRequest))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("用户名或密码错误");
        }
    }

    @Nested
    @DisplayName("POST /api/auth/refresh")
    class Refresh {

        private AuthController.RefreshRequest refreshRequest;

        @BeforeEach
        void setUp() {
            // 生成一个真实的刷新令牌
            String realRefreshToken = tokenProvider.generateRefreshToken(TEST_USERNAME);
            refreshRequest = new AuthController.RefreshRequest(realRefreshToken);
        }

        @Test
        @DisplayName("应成功刷新令牌")
        void shouldRefreshSuccessfully() {
            ResponseEntity<ApiResponse<Map<String, String>>> response =
                    authController.refresh(refreshRequest);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody().getData())
                    .containsEntry("tokenType", "Bearer");
            assertThat(response.getBody().getData().get("accessToken")).isNotNull().isNotEmpty();
            assertThat(response.getBody().getData().get("refreshToken")).isNotNull().isNotEmpty();
        }

        @Test
        @DisplayName("无效的刷新令牌应返回 401")
        void shouldReturn401WhenInvalidToken() {
            refreshRequest = new AuthController.RefreshRequest("invalid-token");

            ResponseEntity<ApiResponse<Map<String, String>>> response =
                    authController.refresh(refreshRequest);

            assertThat(response.getStatusCode().value()).isEqualTo(401);
            assertThat(response.getBody().getCode()).isEqualTo(401);
        }
    }
}
