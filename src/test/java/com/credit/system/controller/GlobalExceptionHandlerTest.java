package com.credit.system.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.credit.system.dto.ApiResponse;
import com.credit.system.exception.BusinessException;
import com.credit.system.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler 单元测试")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Nested
    @DisplayName("ResourceNotFoundException 处理")
    class HandleNotFound {

        @Test
        @DisplayName("应返回 404")
        void shouldReturn404() {
            ResourceNotFoundException ex = new ResourceNotFoundException("客户不存在");

            ResponseEntity<ApiResponse<Void>> response = handler.handleNotFound(ex);

            assertThat(response.getStatusCode().value()).isEqualTo(404);
            assertThat(response.getBody().getCode()).isEqualTo(404);
            assertThat(response.getBody().getMessage()).isEqualTo("客户不存在");
        }
    }

    @Nested
    @DisplayName("BusinessException 处理")
    class HandleBusiness {

        @Test
        @DisplayName("应返回 400")
        void shouldReturn400() {
            BusinessException ex = new BusinessException("业务规则冲突");

            ResponseEntity<ApiResponse<Void>> response = handler.handleBusiness(ex);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getBody().getCode()).isEqualTo(400);
            assertThat(response.getBody().getMessage()).isEqualTo("业务规则冲突");
        }
    }

    @Nested
    @DisplayName("IllegalArgumentException 处理")
    class HandleIllegalArg {

        @Test
        @DisplayName("应返回 400")
        void shouldReturn400() {
            IllegalArgumentException ex = new IllegalArgumentException("参数错误");

            ResponseEntity<ApiResponse<Void>> response = handler.handleIllegalArg(ex);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getBody().getCode()).isEqualTo(400);
            assertThat(response.getBody().getMessage()).isEqualTo("参数错误");
        }
    }

    @Nested
    @DisplayName("BadCredentialsException 处理")
    class HandleBadCredentials {

        @Test
        @DisplayName("应返回 401")
        void shouldReturn401() {
            BadCredentialsException ex = new BadCredentialsException("用户名或密码错误");

            ResponseEntity<ApiResponse<Void>> response = handler.handleBadCredentials(ex);

            assertThat(response.getStatusCode().value()).isEqualTo(401);
            assertThat(response.getBody().getCode()).isEqualTo(401);
            assertThat(response.getBody().getMessage()).isEqualTo("用户名或密码错误");
        }
    }

    @Nested
    @DisplayName("AccessDeniedException 处理")
    class HandleAccessDenied {

        @Test
        @DisplayName("应返回 403")
        void shouldReturn403() {
            AccessDeniedException ex = new AccessDeniedException("需要 ADMIN 角色");

            ResponseEntity<ApiResponse<Void>> response = handler.handleAccessDenied(ex);

            assertThat(response.getStatusCode().value()).isEqualTo(403);
            assertThat(response.getBody().getCode()).isEqualTo(403);
            assertThat(response.getBody().getMessage()).isEqualTo("需要 ADMIN 角色");
        }
    }

    @Nested
    @DisplayName("MethodArgumentNotValidException 处理")
    class HandleValidation {

        @Test
        @DisplayName("应返回 400 并拼接错误信息")
        void shouldReturn400WithJoinedMessages() {
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(null, "target");
            bindingResult.addError(new FieldError("target", "name", "姓名不能为空"));
            bindingResult.addError(new FieldError("target", "phone", "手机号格式不正确"));
            MethodArgumentNotValidException ex =
                    new MethodArgumentNotValidException(null, bindingResult);

            ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(ex);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getBody().getCode()).isEqualTo(400);
            assertThat(response.getBody().getMessage())
                    .contains("姓名不能为空")
                    .contains("手机号格式不正确");
        }
    }

    @Nested
    @DisplayName("通用 Exception 处理")
    class HandleGeneral {

        @Test
        @DisplayName("应返回 500")
        void shouldReturn500() {
            Exception ex = new RuntimeException("未知错误");

            ResponseEntity<ApiResponse<Void>> response = handler.handleGeneral(ex);

            assertThat(response.getStatusCode().value()).isEqualTo(500);
            assertThat(response.getBody().getCode()).isEqualTo(500);
            assertThat(response.getBody().getMessage()).contains("服务器内部错误");
        }
    }
}
