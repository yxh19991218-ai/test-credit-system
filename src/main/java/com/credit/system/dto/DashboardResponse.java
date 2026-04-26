package com.credit.system.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 仪表盘数据响应 DTO —— 聚合首页所需的所有统计指标。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {

    /** 概览卡片 */
    private Overview overview;

    /** 月度趋势（近 12 个月） */
    private List<MonthlyTrend> monthlyTrends;

    /** 产品分布 */
    private List<ProductDistribution> productDistribution;

    /** 合同状态分布 */
    private Map<String, Long> contractStatusDistribution;

    /** 近期到期合同 */
    private List<ContractBrief> upcomingDueContracts;

    /** 逾期预警 */
    private List<OverdueAlert> overdueAlerts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Overview {
        private long totalCustomers;
        private long activeContracts;
        private long pendingApplications;
        private long overdueContracts;
        private BigDecimal totalLoanAmount;
        private BigDecimal totalRemainingPrincipal;
        private BigDecimal totalOverdueAmount;
        private BigDecimal monthlyCollectionRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyTrend {
        private String month;          // "2026-01"
        private long newContracts;
        private BigDecimal loanAmount;
        private BigDecimal repaymentAmount;
        private long newCustomers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductDistribution {
        private String productName;
        private long contractCount;
        private BigDecimal totalAmount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContractBrief {
        private Long contractId;
        private String contractNo;
        private String customerName;
        private BigDecimal totalAmount;
        private BigDecimal remainingPrincipal;
        private String dueDate;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OverdueAlert {
        private Long contractId;
        private String contractNo;
        private String customerName;
        private int overdueDays;
        private BigDecimal overdueAmount;
        private String riskLevel;
    }
}
