package com.credit.system.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.credit.system.dto.ApiResponse;
import com.credit.system.security.JwtTokenProvider;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 认证控制器 —— 提供登录、令牌刷新等接口。
 * <p>
 * 当前为简化实现，使用内存用户 + JWT。
 * 生产环境建议集成 OIDC 提供商（如 Keycloak、Auth0、Azure AD）。
 * </p>
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证管理", description = "用户登录、令牌刷新、登出")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final JwtTokenProvider tokenProvider;

    public AuthController(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "使用用户名和密码登录，返回 JWT 访问令牌和刷新令牌")
    public ResponseEntity<ApiResponse<Map<String, String>>> login(@RequestBody LoginRequest request) {
        log.info("用户登录请求: {}", request.username());

        // 简化实现 —— 仅做演示
        // 生产环境应集成 UserDetailsService + PasswordEncoder 验证
        if (!"admin".equals(request.username()) || !"admin123".equals(request.password())) {
            if (!"user".equals(request.username()) || !"user123".equals(request.password())) {
                log.warn("登录失败 - 用户名或密码错误: {}", request.username());
                throw new BadCredentialsException("用户名或密码错误");
            }
        }

        String role = "admin".equals(request.username()) ? "ADMIN" : "USER";
        String accessToken = tokenProvider.generateAccessToken(request.username(), role);
        String refreshToken = tokenProvider.generateRefreshToken(request.username());

        log.info("登录成功 - 用户: {}, 角色: {}", request.username(), role);

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken,
                "tokenType", "Bearer",
                "username", request.username(),
                "role", role
        )));
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌", description = "使用刷新令牌获取新的访问令牌")
    public ResponseEntity<ApiResponse<Map<String, String>>> refresh(@RequestBody RefreshRequest request) {
        if (!tokenProvider.validateToken(request.refreshToken())) {
            return ResponseEntity.status(401).body(ApiResponse.error("刷新令牌无效或已过期"));
        }

        String username = tokenProvider.getUsernameFromToken(request.refreshToken());
        String role = tokenProvider.getRoleFromToken(request.refreshToken());

        String newAccessToken = tokenProvider.generateAccessToken(username, role);
        String newRefreshToken = tokenProvider.generateRefreshToken(username);

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "accessToken", newAccessToken,
                "refreshToken", newRefreshToken,
                "tokenType", "Bearer"
        )));
    }

    public record LoginRequest(String username, String password) {}

    public record RefreshRequest(String refreshToken) {}
}
