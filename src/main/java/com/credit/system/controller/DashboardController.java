package com.credit.system.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.credit.system.dto.ApiResponse;
import com.credit.system.dto.DashboardResponse;
import com.credit.system.service.DashboardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 仪表盘控制器 —— 提供首页聚合统计指标。
 * <p>
 * 需要 ADMIN 角色访问。
 * </p>
 */
@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "仪表盘", description = "首页统计指标、趋势图表、预警信息")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/overview")
    @Operation(summary = "获取仪表盘概览", description = "返回首页所需的所有统计指标，包括概览卡片、月度趋势、产品分布等")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DashboardResponse>> getOverview() {
        DashboardResponse data = dashboardService.getDashboardOverview();
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
