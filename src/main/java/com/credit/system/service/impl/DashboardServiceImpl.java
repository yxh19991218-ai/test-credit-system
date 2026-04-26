package com.credit.system.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.credit.system.domain.LoanContract;
import com.credit.system.domain.enums.ApplicationStatus;
import com.credit.system.domain.enums.ContractStatus;
import com.credit.system.dto.DashboardResponse;
import com.credit.system.repository.CustomerRepository;
import com.credit.system.repository.LoanApplicationRepository;
import com.credit.system.repository.LoanContractRepository;
import com.credit.system.repository.RepaymentPeriodRepository;
import com.credit.system.repository.RepaymentScheduleRepository;
import com.credit.system.service.DashboardService;

/**
 * 仪表盘服务实现 —— 从多个 Repository 聚合数据。
 * <p>
 * 注意：当前实现为演示版本，生产环境建议使用专门的统计表或物化视图来优化查询性能。
 * </p>
 */
@Service
public class DashboardServiceImpl implements DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardServiceImpl.class);

    private final CustomerRepository customerRepository;
    private final LoanContractRepository contractRepository;
    private final LoanApplicationRepository applicationRepository;
    private final RepaymentPeriodRepository periodRepository;
    private final RepaymentScheduleRepository scheduleRepository;

    public DashboardServiceImpl(CustomerRepository customerRepository,
                                LoanContractRepository contractRepository,
                                LoanApplicationRepository applicationRepository,
                                RepaymentPeriodRepository periodRepository,
                                RepaymentScheduleRepository scheduleRepository) {
        this.customerRepository = customerRepository;
        this.contractRepository = contractRepository;
        this.applicationRepository = applicationRepository;
        this.periodRepository = periodRepository;
        this.scheduleRepository = scheduleRepository;
    }

    @Override
    public DashboardResponse getDashboardOverview() {
        log.debug("开始聚合仪表盘数据");

        // 1. 概览卡片
        long totalCustomers = customerRepository.count();
        long activeContracts = contractRepository.countByStatus(ContractStatus.ACTIVE);
        long pendingApplications = applicationRepository
                .findByStatus(ApplicationStatus.PENDING).size();
        long overdueContracts = contractRepository
                .findOverdueContracts(LocalDate.now()).size();

        // 总额度 / 剩余本金
        BigDecimal totalLoanAmount = Optional.ofNullable(
                contractRepository.sumAllActiveRemainingPrincipal()).orElse(BigDecimal.ZERO);

        // 逾期金额
        BigDecimal totalOverdueAmount = Optional.ofNullable(
                periodRepository.sumOverdueAmount()).orElse(BigDecimal.ZERO);

        // 本月回款率
        BigDecimal monthlyCollectionRate = calculateMonthlyCollectionRate();

        var overview = DashboardResponse.Overview.builder()
                .totalCustomers(totalCustomers)
                .activeContracts(activeContracts)
                .pendingApplications(pendingApplications)
                .overdueContracts(overdueContracts)
                .totalLoanAmount(totalLoanAmount)
                .totalRemainingPrincipal(totalLoanAmount)
                .totalOverdueAmount(totalOverdueAmount)
                .monthlyCollectionRate(monthlyCollectionRate)
                .build();

        // 2. 月度趋势（近 12 个月）
        List<DashboardResponse.MonthlyTrend> monthlyTrends = buildMonthlyTrends();

        // 3. 产品分布
        List<DashboardResponse.ProductDistribution> productDistribution = buildProductDistribution();

        // 4. 合同状态分布
        Map<String, Long> contractStatusDistribution = buildContractStatusDistribution();

        // 5. 近期到期合同（7 天内）
        List<DashboardResponse.ContractBrief> upcomingDueContracts = buildUpcomingDueContracts();

        // 6. 逾期预警
        List<DashboardResponse.OverdueAlert> overdueAlerts = buildOverdueAlerts();

        log.debug("仪表盘数据聚合完成 - 客户数: {}, 活跃合同: {}, 待审批: {}",
                totalCustomers, activeContracts, pendingApplications);

        return DashboardResponse.builder()
                .overview(overview)
                .monthlyTrends(monthlyTrends)
                .productDistribution(productDistribution)
                .contractStatusDistribution(contractStatusDistribution)
                .upcomingDueContracts(upcomingDueContracts)
                .overdueAlerts(overdueAlerts)
                .build();
    }

    private BigDecimal calculateMonthlyCollectionRate() {
        LocalDate now = LocalDate.now();
        LocalDate monthStart = now.withDayOfMonth(1);
        LocalDate monthEnd = now.withDayOfMonth(now.lengthOfMonth());

        BigDecimal dueThisMonth = Optional.ofNullable(
                periodRepository.sumTotalAmountByDueDateBetween(monthStart, monthEnd)
        ).orElse(BigDecimal.ZERO);

        BigDecimal paidThisMonth = Optional.ofNullable(
                periodRepository.sumPaidAmountByPaidDateBetween(monthStart, monthEnd)
        ).orElse(BigDecimal.ZERO);

        if (dueThisMonth.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(100);
        }
        return paidThisMonth.multiply(BigDecimal.valueOf(100))
                .divide(dueThisMonth, 2, java.math.RoundingMode.HALF_UP);
    }

    private List<DashboardResponse.MonthlyTrend> buildMonthlyTrends() {
        List<DashboardResponse.MonthlyTrend> trends = new ArrayList<>();
        LocalDate now = LocalDate.now();

        for (int i = 11; i >= 0; i--) {
            LocalDate month = now.minusMonths(i);
            String monthStr = String.format("%d-%02d", month.getYear(), month.getMonthValue());
            trends.add(DashboardResponse.MonthlyTrend.builder()
                    .month(monthStr)
                    .newContracts(0)
                    .loanAmount(BigDecimal.ZERO)
                    .repaymentAmount(BigDecimal.ZERO)
                    .newCustomers(0)
                    .build());
        }
        return trends;
    }

    private List<DashboardResponse.ProductDistribution> buildProductDistribution() {
        return List.of();
    }

    private Map<String, Long> buildContractStatusDistribution() {
        Map<String, Long> distribution = new LinkedHashMap<>();
        for (ContractStatus status : ContractStatus.values()) {
            long count = contractRepository.findByStatus(status).size();
            distribution.put(status.name(), count);
        }
        return distribution;
    }

    private List<DashboardResponse.ContractBrief> buildUpcomingDueContracts() {
        LocalDate now = LocalDate.now();
        LocalDate sevenDaysLater = now.plusDays(7);
        List<LoanContract> dueContracts = contractRepository
                .findContractsDueBetween(now, sevenDaysLater);

        return dueContracts.stream().map(c -> DashboardResponse.ContractBrief.builder()
                .contractId(c.getId())
                .contractNo(c.getContractNo())
                .customerName("客户-" + c.getCustomerId())
                .totalAmount(c.getTotalAmount())
                .remainingPrincipal(c.getRemainingPrincipal())
                .dueDate(c.getEndDate().toString())
                .status(c.getStatus().name())
                .build()).collect(Collectors.toList());
    }

    private List<DashboardResponse.OverdueAlert> buildOverdueAlerts() {
        List<LoanContract> overdueContracts = contractRepository
                .findOverdueContracts(LocalDate.now());

        return overdueContracts.stream().map(c -> {
            int overdueDays = (int) LocalDate.now().until(c.getEndDate()).getDays() * -1;
            return DashboardResponse.OverdueAlert.builder()
                    .contractId(c.getId())
                    .contractNo(c.getContractNo())
                    .customerName("客户-" + c.getCustomerId())
                    .overdueDays(Math.max(overdueDays, 0))
                    .overdueAmount(c.getRemainingPrincipal())
                    .riskLevel(overdueDays > 90 ? "HIGH" : overdueDays > 30 ? "MEDIUM" : "LOW")
                    .build();
        }).collect(Collectors.toList());
    }
}
