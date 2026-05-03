package com.credit.system.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.credit.system.domain.LoanApplication;
import com.credit.system.domain.enums.ApplicationStatus;
import com.credit.system.dto.ApiResponse;
import com.credit.system.dto.ApplicationReviewRequest;
import com.credit.system.dto.LoanApplicationRequest;
import com.credit.system.dto.LoanApplicationResponse;
import com.credit.system.exception.ResourceNotFoundException;
import com.credit.system.service.LoanApplicationService;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoanApplicationController 单元测试")
class LoanApplicationControllerTest {

    @Mock
    private LoanApplicationService applicationService;

    @InjectMocks
    private LoanApplicationController applicationController;

    private LoanApplication sampleApplication;

    @BeforeEach
    void setUp() {
        sampleApplication = new LoanApplication();
        sampleApplication.setId(1L);
        sampleApplication.setCustomerId(1L);
        sampleApplication.setProductId(1L);
        sampleApplication.setApplyAmount(new BigDecimal("100000"));
        sampleApplication.setApplyTerm(12);
        sampleApplication.setPurpose("装修");
        sampleApplication.setStatus(ApplicationStatus.DRAFT);
        sampleApplication.setApplicationDate(LocalDateTime.now());
    }

    @Nested
    @DisplayName("POST /api/applications")
    class CreateApplication {

        @Test
        @DisplayName("应成功创建并返回200")
        void shouldCreateSuccessfully() {
            given(applicationService.createApplication(any(LoanApplication.class), anyString()))
                    .willReturn(sampleApplication);

            LoanApplicationRequest request = new LoanApplicationRequest();

            ResponseEntity<ApiResponse<LoanApplicationResponse>> response =
                    applicationController.createApplication(request, "SYSTEM");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getCode()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("GET /api/applications/{id}")
    class GetApplication {

        @Test
        @DisplayName("应返回申请信息")
        void shouldReturnApplication() {
            given(applicationService.getApplicationById(1L)).willReturn(Optional.of(sampleApplication));

            ResponseEntity<ApiResponse<LoanApplicationResponse>> response =
                    applicationController.getApplication(1L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData().getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("不存在的申请应抛出异常")
        void shouldThrowExceptionWhenNotFound() {
            given(applicationService.getApplicationById(999L)).willReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> applicationController.getApplication(999L));
        }
    }

    @Nested
    @DisplayName("GET /api/applications （分页）")
    class ListApplications {

        @Test
        @DisplayName("应返回分页结果")
        void shouldReturnPagedResult() {
            Page<LoanApplication> page = new PageImpl<>(Collections.singletonList(sampleApplication));
            given(applicationService.getApplicationList(any(), any(), any(), anyInt(), anyInt()))
                    .willReturn(page);

            ResponseEntity<ApiResponse<Page<LoanApplicationResponse>>> response =
                    applicationController.listApplications(null, null, null, 0, 10);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData().getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("PUT /api/applications/{id}")
    class UpdateApplication {

        @Test
        @DisplayName("应成功更新并返回200")
        void shouldUpdateSuccessfully() {
            given(applicationService.updateApplication(anyLong(), any(LoanApplication.class)))
                    .willReturn(sampleApplication);

            LoanApplicationRequest request = new LoanApplicationRequest();

            ResponseEntity<ApiResponse<LoanApplicationResponse>> response =
                    applicationController.updateApplication(1L, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Nested
    @DisplayName("POST /api/applications/{id}/submit")
    class SubmitApplication {

        @Test
        @DisplayName("应成功提交并返回200")
        void shouldSubmitSuccessfully() {
            doNothing().when(applicationService).submitApplication(1L);

            ResponseEntity<ApiResponse<String>> response =
                    applicationController.submitApplication(1L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getCode()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("POST /api/applications/{id}/review")
    class ReviewApplication {

        @Test
        @DisplayName("应成功审核并返回200")
        void shouldReviewSuccessfully() {
            doNothing().when(applicationService).reviewApplication(
                    anyLong(), any(ApplicationStatus.class), anyString(),
                    anyString(), any(), any(), any());

            ApplicationReviewRequest request = new ApplicationReviewRequest();
            request.setDecision("APPROVED");
            request.setReviewer("审核员");
            request.setComments("通过");

            ResponseEntity<ApiResponse<String>> response =
                    applicationController.reviewApplication(1L, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Nested
    @DisplayName("POST /api/applications/{id}/cancel")
    class CancelApplication {

        @Test
        @DisplayName("应成功取消并返回200")
        void shouldCancelSuccessfully() {
            doNothing().when(applicationService).cancelApplication(1L, "用户取消");

            ResponseEntity<ApiResponse<String>> response =
                    applicationController.cancelApplication(1L, "用户取消");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }
}
