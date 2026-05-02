package com.credit.system.audit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityAspect 单元测试")
class SecurityAspectTest {

    private SecurityAspect securityAspect;

    @BeforeEach
    void setUp() {
        securityAspect = new SecurityAspect();
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("checkAdmin")
    class CheckAdmin {

        @Test
        @DisplayName("ADMIN 角色应通过检查")
        void shouldPassForAdminRole() {
            var authentication = new UsernamePasswordAuthenticationToken(
                    "admin", null,
                    List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 不应抛出异常
            securityAspect.checkAdmin(null);
        }

        @Test
        @DisplayName("USER 角色应抛出 AccessDeniedException")
        void shouldThrowForUserRole() {
            var authentication = new UsernamePasswordAuthenticationToken(
                    "zhangsan", null,
                    List.of(new SimpleGrantedAuthority("ROLE_USER")));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            assertThatThrownBy(() -> securityAspect.checkAdmin(null))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("需要 ADMIN 角色");
        }

        @Test
        @DisplayName("未认证用户应抛出 AccessDeniedException")
        void shouldThrowForUnauthenticatedUser() {
            SecurityContextHolder.getContext().setAuthentication(null);

            assertThatThrownBy(() -> securityAspect.checkAdmin(null))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("需要 ADMIN 角色");
        }

        @Test
        @DisplayName("匿名用户应抛出 AccessDeniedException")
        void shouldThrowForAnonymousUser() {
            var authentication = new UsernamePasswordAuthenticationToken(
                    "anonymousUser", null, List.of());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            assertThatThrownBy(() -> securityAspect.checkAdmin(null))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("需要 ADMIN 角色");
        }
    }
}
