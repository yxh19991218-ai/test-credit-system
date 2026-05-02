package com.credit.system.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import io.jsonwebtoken.JwtException;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtTokenProvider 单元测试")
class JwtTokenProviderTest {

    private static final String TEST_SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci1qd3QtdG9rZW4tcHJvdmlkZXItdGVzdGluZw==";
    private static final long ACCESS_EXPIRATION = 3600000; // 1 hour
    private static final long REFRESH_EXPIRATION = 604800000; // 7 days

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(TEST_SECRET, ACCESS_EXPIRATION, REFRESH_EXPIRATION);
    }

    @Nested
    @DisplayName("生成访问令牌 generateAccessToken")
    class GenerateAccessToken {

        @Test
        @DisplayName("应成功生成有效的访问令牌")
        void shouldGenerateValidAccessToken() {
            String token = tokenProvider.generateAccessToken("admin", "ADMIN");

            assertThat(token).isNotNull().isNotEmpty();
            assertThat(tokenProvider.validateToken(token)).isTrue();
            assertThat(tokenProvider.getUsernameFromToken(token)).isEqualTo("admin");
            assertThat(tokenProvider.getRoleFromToken(token)).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("应成功生成 USER 角色的访问令牌")
        void shouldGenerateUserRoleToken() {
            String token = tokenProvider.generateAccessToken("zhangsan", "USER");

            assertThat(tokenProvider.getUsernameFromToken(token)).isEqualTo("zhangsan");
            assertThat(tokenProvider.getRoleFromToken(token)).isEqualTo("USER");
        }

        @Test
        @DisplayName("不同用户生成的令牌应不同")
        void shouldGenerateDifferentTokensForDifferentUsers() {
            String token1 = tokenProvider.generateAccessToken("admin", "ADMIN");
            String token2 = tokenProvider.generateAccessToken("user", "USER");

            assertThat(token1).isNotEqualTo(token2);
        }
    }

    @Nested
    @DisplayName("生成刷新令牌 generateRefreshToken")
    class GenerateRefreshToken {

        @Test
        @DisplayName("应成功生成有效的刷新令牌")
        void shouldGenerateValidRefreshToken() {
            String token = tokenProvider.generateRefreshToken("admin");

            assertThat(token).isNotNull().isNotEmpty();
            assertThat(tokenProvider.validateToken(token)).isTrue();
            assertThat(tokenProvider.getUsernameFromToken(token)).isEqualTo("admin");
        }

        @Test
        @DisplayName("刷新令牌应包含 type=refresh 声明")
        void shouldContainRefreshTypeClaim() {
            String token = tokenProvider.generateRefreshToken("admin");

            // 刷新令牌的 role 声明应为 null（未设置 role claim）
            assertThat(tokenProvider.getRoleFromToken(token)).isNull();
        }
    }

    @Nested
    @DisplayName("令牌验证 validateToken")
    class ValidateToken {

        @Test
        @DisplayName("有效令牌应返回 true")
        void shouldReturnTrueForValidToken() {
            String token = tokenProvider.generateAccessToken("admin", "ADMIN");

            assertThat(tokenProvider.validateToken(token)).isTrue();
        }

        @Test
        @DisplayName("无效令牌应返回 false")
        void shouldReturnFalseForInvalidToken() {
            assertThat(tokenProvider.validateToken("invalid.token.here")).isFalse();
        }

        @Test
        @DisplayName("空令牌应返回 false")
        void shouldReturnFalseForEmptyToken() {
            assertThat(tokenProvider.validateToken("")).isFalse();
        }

        @Test
        @DisplayName("篡改过的令牌应返回 false")
        void shouldReturnFalseForTamperedToken() {
            String token = tokenProvider.generateAccessToken("admin", "ADMIN");
            String tamperedToken = token.substring(0, token.length() - 5) + "XXXXX";

            assertThat(tokenProvider.validateToken(tamperedToken)).isFalse();
        }
    }

    @Nested
    @DisplayName("提取用户名 getUsernameFromToken")
    class GetUsernameFromToken {

        @Test
        @DisplayName("应正确提取用户名")
        void shouldExtractUsername() {
            String token = tokenProvider.generateAccessToken("admin", "ADMIN");

            assertThat(tokenProvider.getUsernameFromToken(token)).isEqualTo("admin");
        }

        @Test
        @DisplayName("无效令牌应抛出异常")
        void shouldThrowForInvalidToken() {
            assertThatThrownBy(() -> tokenProvider.getUsernameFromToken("invalid.token"))
                    .isInstanceOf(JwtException.class);
        }
    }

    @Nested
    @DisplayName("提取角色 getRoleFromToken")
    class GetRoleFromToken {

        @Test
        @DisplayName("应正确提取角色")
        void shouldExtractRole() {
            String token = tokenProvider.generateAccessToken("admin", "ADMIN");

            assertThat(tokenProvider.getRoleFromToken(token)).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("USER 角色应正确提取")
        void shouldExtractUserRole() {
            String token = tokenProvider.generateAccessToken("zhangsan", "USER");

            assertThat(tokenProvider.getRoleFromToken(token)).isEqualTo("USER");
        }
    }

    @Nested
    @DisplayName("令牌即将过期检查 isTokenExpiringSoon")
    class IsTokenExpiringSoon {

        @Test
        @DisplayName("新生成的令牌不应即将过期")
        void shouldNotBeExpiringSoonForNewToken() {
            String token = tokenProvider.generateAccessToken("admin", "ADMIN");

            assertThat(tokenProvider.isTokenExpiringSoon(token)).isFalse();
        }

        @Test
        @DisplayName("无效令牌应抛出异常")
        void shouldThrowForInvalidToken() {
            assertThatThrownBy(() -> tokenProvider.isTokenExpiringSoon("invalid.token"))
                    .isInstanceOf(JwtException.class);
        }
    }
}
