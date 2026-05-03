package com.credit.system.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;

import com.credit.system.dto.ApiResponse;
import com.credit.system.security.JwtTokenProvider;

@DisplayName("AuthController 单元测试")
class AuthControllerTest {

    private JwtTokenProvider tokenProvider;
    private AuthController authController;

    private static final String TEST_USERNAME = "admin";
    private static final String TEST_PASSWORD = "admin123";
    private static final String TEST_ACCESS_TOKEN = "test-access-token";
    private static final String TEST_REFRESH_TOKEN = "test-refresh-token";

    @BeforeEach
    void setUp() {
        // JwtTokenProvider 使用真实实例
        String testSecret = "dGVzdC1zZWNyZXQta2V5LWZvci1qd3QtdG9rZW4tcHJvdmlkZXItdGVzdA==";
        tokenProvider = new JwtTokenProvider(testSecret, 3600000, 604800000);
        authController = new AuthController(tokenProvider);
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("admin 应成功登录并返回令牌")
        void shouldLoginSuccessfully() {
            AuthController.LoginRequest loginRequest =
                    new AuthController.LoginRequest("admin", "admin123");

            ResponseEntity<ApiResponse<Map<String, String>>> response =
                    authController.login(loginRequest);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody().getCode()).isEqualTo(200);
            assertThat(response.getBody().getData())
                    .containsEntry("tokenType", "Bearer")
                    .containsEntry("username", "admin")
                    .containsEntry("role", "ADMIN");
            assertThat(response.getBody().getData().get("accessToken")).isNotNull().isNotEmpty();
            assertThat(response.getBody().getData().get("refreshToken")).isNotNull().isNotEmpty();
        }

        @Test
        @DisplayName("user 登录应返回 USER 角色")
        void shouldReturnUserRole() {
            AuthController.LoginRequest loginRequest =
                    new AuthController.LoginRequest("user", "user123");

            ResponseEntity<ApiResponse<Map<String, String>>> response =
                    authController.login(loginRequest);

            assertThat(response.getBody().getData())
                    .containsEntry("role", "USER");
        }

        @Test
        @DisplayName("密码错误应抛出 BadCredentialsException")
        void shouldThrowWhenBadCredentials() {
            AuthController.LoginRequest loginRequest =
                    new AuthController.LoginRequest("admin", "wrongpassword");

            assertThatThrownBy(() -> authController.login(loginRequest))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("用户名或密码错误");
        }
    }

    @Nested
    @DisplayName("POST /api/auth/refresh")
    class Refresh {

        @Test
        @DisplayName("应成功刷新令牌")
        void shouldRefreshSuccessfully() {
            // 生成一个真实的刷新令牌
            String realRefreshToken = tokenProvider.generateRefreshToken("admin");
            AuthController.RefreshRequest refreshRequest =
                    new AuthController.RefreshRequest(realRefreshToken);

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
            AuthController.RefreshRequest refreshRequest =
                    new AuthController.RefreshRequest("invalid-token");

            ResponseEntity<ApiResponse<Map<String, String>>> response =
                    authController.refresh(refreshRequest);

            assertThat(response.getStatusCode().value()).isEqualTo(401);
            assertThat(response.getBody().getCode()).isEqualTo(401);
        }
    }
}
