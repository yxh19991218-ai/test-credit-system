package com.credit.system.service;

import com.credit.system.dto.DashboardResponse;

/**
 * 仪表盘服务接口 —— 聚合首页统计指标。
 */
public interface DashboardService {

    /**
     * 获取仪表盘概览数据
     */
    DashboardResponse getDashboardOverview();
}
