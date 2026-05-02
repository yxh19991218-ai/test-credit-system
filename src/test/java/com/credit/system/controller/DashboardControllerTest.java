package com.credit.system.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.credit.system.dto.ApiResponse;
import com.credit.system.dto.DashboardResponse;
import com.credit.system.service.DashboardService;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardController 单元测试")
class DashboardControllerTest {

    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private DashboardController dashboardController;

    @Nested
    @DisplayName("GET /api/dashboard/overview")
    class GetOverview {

        @Test
        @DisplayName("应返回仪表盘概览数据")
        void shouldReturnOverview() {
            DashboardResponse mockResponse = new DashboardResponse();
            given(dashboardService.getDashboardOverview()).willReturn(mockResponse);

            ResponseEntity<ApiResponse<DashboardResponse>> response =
                    dashboardController.getOverview();

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody().getCode()).isEqualTo(200);
            assertThat(response.getBody().getData()).isSameAs(mockResponse);
        }
    }
}
