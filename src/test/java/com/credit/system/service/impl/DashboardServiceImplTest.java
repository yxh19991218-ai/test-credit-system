package com.credit.system.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.credit.system.domain.LoanContract;
import com.credit.system.domain.enums.ApplicationStatus;
import com.credit.system.domain.enums.ContractStatus;
import com.credit.system.dto.DashboardResponse;
import com.credit.system.repository.CustomerRepository;
import com.credit.system.repository.LoanApplicationRepository;
import com.credit.system.repository.LoanContractRepository;
import com.credit.system.repository.RepaymentPeriodRepository;
import com.credit.system.repository.RepaymentScheduleRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardServiceImpl 单元测试")
class DashboardServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private LoanContractRepository contractRepository;

    @Mock
    private LoanApplicationRepository applicationRepository;

    @Mock
    private RepaymentPeriodRepository periodRepository;

    @Mock
    private RepaymentScheduleRepository scheduleRepository;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    private LoanContract sampleContract;

    @BeforeEach
    void setUp() {
        sampleContract = new LoanContract();
        sampleContract.setId(1L);
        sampleContract.setContractNo("LOAN20260426001");
        sampleContract.setCustomerId(1L);
        sampleContract.setTotalAmount(new BigDecimal("100000"));
        sampleContract.setRemainingPrincipal(new BigDecimal("80000"));
        sampleContract.setStatus(ContractStatus.ACTIVE);
        sampleContract.setStartDate(LocalDate.now().minusMonths(3));
        sampleContract.setEndDate(LocalDate.now().plusMonths(9));
    }

    @Nested
    @DisplayName("获取仪表盘概览 getDashboardOverview")
    class GetDashboardOverview {

        @Test
        @DisplayName("应返回完整的仪表盘数据")
        void shouldReturnCompleteDashboard() {
            // 概览卡片数据
            given(customerRepository.count()).willReturn(100L);
            given(contractRepository.countByStatus(ContractStatus.ACTIVE)).willReturn(50L);
            given(applicationRepository.findByStatus(ApplicationStatus.PENDING))
                    .willReturn(Collections.nCopies(10, null));
            given(contractRepository.findOverdueContracts(any(LocalDate.class)))
                    .willReturn(Collections.nCopies(5, sampleContract));
            given(contractRepository.sumAllActiveRemainingPrincipal())
                    .willReturn(new BigDecimal("5000000"));
            given(periodRepository.sumOverdueAmount())
                    .willReturn(new BigDecimal("50000"));

            // 月度回款率
            given(periodRepository.sumTotalAmountByDueDateBetween(any(LocalDate.class), any(LocalDate.class)))
                    .willReturn(new BigDecimal("200000"));
            given(periodRepository.sumPaidAmountByPaidDateBetween(any(LocalDate.class), any(LocalDate.class)))
                    .willReturn(new BigDecimal("180000"));

            // 合同状态分布 — 使用 findByStatus 匹配实际实现
            for (ContractStatus status : ContractStatus.values()) {
                given(contractRepository.findByStatus(status)).willReturn(Collections.emptyList());
            }
            given(contractRepository.findByStatus(ContractStatus.ACTIVE))
                    .willReturn(Collections.nCopies(50, sampleContract));
            given(contractRepository.findByStatus(ContractStatus.SETTLED))
                    .willReturn(Collections.nCopies(30, sampleContract));

            // 近期到期合同
            given(contractRepository.findContractsDueBetween(any(LocalDate.class), any(LocalDate.class)))
                    .willReturn(Collections.singletonList(sampleContract));

            DashboardResponse response = dashboardService.getDashboardOverview();

            // 验证概览卡片
            assertThat(response.getOverview()).isNotNull();
            assertThat(response.getOverview().getTotalCustomers()).isEqualTo(100);
            assertThat(response.getOverview().getActiveContracts()).isEqualTo(50);
            assertThat(response.getOverview().getPendingApplications()).isEqualTo(10);
            assertThat(response.getOverview().getOverdueContracts()).isEqualTo(5);
            assertThat(response.getOverview().getTotalLoanAmount())
                    .isEqualByComparingTo(new BigDecimal("5000000"));
            assertThat(response.getOverview().getTotalOverdueAmount())
                    .isEqualByComparingTo(new BigDecimal("50000"));

            // 验证月度回款率：180000/200000*100 = 90.00
            assertThat(response.getOverview().getMonthlyCollectionRate())
                    .isEqualByComparingTo(new BigDecimal("90.00"));

            // 验证月度趋势（12个月，所有值均为0）
            assertThat(response.getMonthlyTrends()).hasSize(12);
            response.getMonthlyTrends().forEach(trend -> {
                assertThat(trend.getMonth()).isNotNull();
                assertThat(trend.getNewContracts()).isEqualTo(0);
                assertThat(trend.getNewCustomers()).isEqualTo(0);
                assertThat(trend.getLoanAmount()).isEqualByComparingTo(BigDecimal.ZERO);
                assertThat(trend.getRepaymentAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            });

            // 验证合同状态分布
            assertThat(response.getContractStatusDistribution())
                    .containsEntry("ACTIVE", 50L)
                    .containsEntry("SETTLED", 30L);
            // 确保所有状态都有条目
            for (ContractStatus status : ContractStatus.values()) {
                assertThat(response.getContractStatusDistribution()).containsKey(status.name());
            }

            // 验证近期到期合同
            assertThat(response.getUpcomingDueContracts()).hasSize(1);
            DashboardResponse.ContractBrief brief = response.getUpcomingDueContracts().get(0);
            assertThat(brief.getContractId()).isEqualTo(1L);
            assertThat(brief.getContractNo()).isEqualTo("LOAN20260426001");
            assertThat(brief.getTotalAmount()).isEqualByComparingTo(new BigDecimal("100000"));

            // 验证逾期预警
            assertThat(response.getOverdueAlerts()).hasSize(5);
        }

        @Test
        @DisplayName("无数据时应返回零值")
        void shouldReturnZeroValuesWhenNoData() {
            given(customerRepository.count()).willReturn(0L);
            given(contractRepository.countByStatus(ContractStatus.ACTIVE)).willReturn(0L);
            given(applicationRepository.findByStatus(ApplicationStatus.PENDING))
                    .willReturn(Collections.emptyList());
            given(contractRepository.findOverdueContracts(any(LocalDate.class)))
                    .willReturn(Collections.emptyList());
            given(contractRepository.sumAllActiveRemainingPrincipal()).willReturn(null);
            given(periodRepository.sumOverdueAmount()).willReturn(null);

            // 回款率：无应还款时返回100%
            given(periodRepository.sumTotalAmountByDueDateBetween(any(LocalDate.class), any(LocalDate.class)))
                    .willReturn(BigDecimal.ZERO);

            // 合同状态分布
            for (ContractStatus status : ContractStatus.values()) {
                given(contractRepository.findByStatus(status)).willReturn(Collections.emptyList());
            }

            // 近期到期合同
            given(contractRepository.findContractsDueBetween(any(LocalDate.class), any(LocalDate.class)))
                    .willReturn(Collections.emptyList());

            DashboardResponse response = dashboardService.getDashboardOverview();

            assertThat(response.getOverview().getTotalCustomers()).isZero();
            assertThat(response.getOverview().getActiveContracts()).isZero();
            assertThat(response.getOverview().getPendingApplications()).isZero();
            assertThat(response.getOverview().getOverdueContracts()).isZero();
            assertThat(response.getOverview().getTotalLoanAmount())
                    .isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(response.getOverview().getTotalOverdueAmount())
                    .isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(response.getOverview().getMonthlyCollectionRate())
                    .isEqualByComparingTo(new BigDecimal("100"));

            assertThat(response.getMonthlyTrends()).hasSize(12);
            assertThat(response.getUpcomingDueContracts()).isEmpty();
            assertThat(response.getOverdueAlerts()).isEmpty();
        }
    }

    @Nested
    @DisplayName("逾期预警 buildOverdueAlerts")
    class BuildOverdueAlerts {

        @Test
        @DisplayName("应正确计算逾期天数和风险等级")
        void shouldCalculateOverdueDaysAndRiskLevel() {
            // 创建一个已过期的合同
            LoanContract overdueContract = new LoanContract();
            overdueContract.setId(1L);
            overdueContract.setContractNo("LOAN20260426001");
            overdueContract.setCustomerId(1L);
            overdueContract.setTotalAmount(new BigDecimal("100000"));
            overdueContract.setRemainingPrincipal(new BigDecimal("80000"));
            overdueContract.setStatus(ContractStatus.ACTIVE);
            overdueContract.setEndDate(LocalDate.now().minusDays(45)); // 逾期45天

            given(customerRepository.count()).willReturn(1L);
            given(contractRepository.countByStatus(ContractStatus.ACTIVE)).willReturn(1L);
            given(applicationRepository.findByStatus(ApplicationStatus.PENDING))
                    .willReturn(Collections.emptyList());
            given(contractRepository.findOverdueContracts(any(LocalDate.class)))
                    .willReturn(Collections.singletonList(overdueContract));
            given(contractRepository.sumAllActiveRemainingPrincipal())
                    .willReturn(new BigDecimal("100000"));
            given(periodRepository.sumOverdueAmount()).willReturn(new BigDecimal("5000"));

            // 回款率
            given(periodRepository.sumTotalAmountByDueDateBetween(any(LocalDate.class), any(LocalDate.class)))
                    .willReturn(new BigDecimal("100000"));
            given(periodRepository.sumPaidAmountByPaidDateBetween(any(LocalDate.class), any(LocalDate.class)))
                    .willReturn(new BigDecimal("80000"));

            // 合同状态分布
            for (ContractStatus status : ContractStatus.values()) {
                given(contractRepository.findByStatus(status)).willReturn(Collections.emptyList());
            }

            // 近期到期合同
            given(contractRepository.findContractsDueBetween(any(LocalDate.class), any(LocalDate.class)))
                    .willReturn(Collections.emptyList());

            DashboardResponse response = dashboardService.getDashboardOverview();

            assertThat(response.getOverdueAlerts()).hasSize(1);
            DashboardResponse.OverdueAlert alert = response.getOverdueAlerts().get(0);
            assertThat(alert.getOverdueDays()).isGreaterThanOrEqualTo(45);
            assertThat(alert.getRiskLevel()).isEqualTo("MEDIUM"); // 逾期45天 > 30天
            assertThat(alert.getOverdueAmount()).isEqualByComparingTo(new BigDecimal("80000"));
        }

        @Test
        @DisplayName("逾期超过90天应标记为高风险")
        void shouldMarkHighRiskWhenOver90Days() {
            LoanContract overdueContract = new LoanContract();
            overdueContract.setId(1L);
            overdueContract.setContractNo("LOAN20260426001");
            overdueContract.setCustomerId(1L);
            overdueContract.setTotalAmount(new BigDecimal("100000"));
            overdueContract.setRemainingPrincipal(new BigDecimal("80000"));
            overdueContract.setStatus(ContractStatus.ACTIVE);
            overdueContract.setEndDate(LocalDate.now().minusDays(100)); // 逾期100天

            given(customerRepository.count()).willReturn(1L);
            given(contractRepository.countByStatus(ContractStatus.ACTIVE)).willReturn(1L);
            given(applicationRepository.findByStatus(ApplicationStatus.PENDING))
                    .willReturn(Collections.emptyList());
            given(contractRepository.findOverdueContracts(any(LocalDate.class)))
                    .willReturn(Collections.singletonList(overdueContract));
            given(contractRepository.sumAllActiveRemainingPrincipal())
                    .willReturn(new BigDecimal("100000"));
            given(periodRepository.sumOverdueAmount()).willReturn(new BigDecimal("5000"));

            given(periodRepository.sumTotalAmountByDueDateBetween(any(LocalDate.class), any(LocalDate.class)))
                    .willReturn(new BigDecimal("100000"));
            given(periodRepository.sumPaidAmountByPaidDateBetween(any(LocalDate.class), any(LocalDate.class)))
                    .willReturn(new BigDecimal("80000"));

            // 合同状态分布
            for (ContractStatus status : ContractStatus.values()) {
                given(contractRepository.findByStatus(status)).willReturn(Collections.emptyList());
            }

            given(contractRepository.findContractsDueBetween(any(LocalDate.class), any(LocalDate.class)))
                    .willReturn(Collections.emptyList());

            DashboardResponse response = dashboardService.getDashboardOverview();

            DashboardResponse.OverdueAlert alert = response.getOverdueAlerts().get(0);
            assertThat(alert.getRiskLevel()).isEqualTo("HIGH");
        }
    }
}
