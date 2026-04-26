   package com.credit.system.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.credit.system.domain.LoanContract;
import com.credit.system.domain.RepaymentPeriod;
import com.credit.system.domain.RepaymentPeriodStatus;
import com.credit.system.domain.RepaymentSchedule;
import com.credit.system.domain.enums.RepaymentMethod;
import com.credit.system.domain.enums.ScheduleStatus;
import com.credit.system.exception.BusinessException;
import com.credit.system.exception.ResourceNotFoundException;
import com.credit.system.repository.LoanContractRepository;
import com.credit.system.repository.RepaymentPeriodRepository;
import com.credit.system.repository.RepaymentScheduleRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("RepaymentScheduleServiceImpl 单元测试")
class RepaymentScheduleServiceImplTest {

    @Mock
    private RepaymentScheduleRepository scheduleRepository;

    @Mock
    private RepaymentPeriodRepository periodRepository;

    @Mock
    private LoanContractRepository contractRepository;

    @InjectMocks
    private RepaymentScheduleServiceImpl scheduleService;

    private LoanContract sampleContract;
    private RepaymentSchedule sampleSchedule;
    private RepaymentPeriod samplePeriod;

    @BeforeEach
    void setUp() {
        sampleContract = new LoanContract();
        sampleContract.setId(1L);
        sampleContract.setContractNo("LOAN20260426001");
        sampleContract.setTotalAmount(new BigDecimal("120000"));
        sampleContract.setInterestRate(new BigDecimal("0.12"));
        sampleContract.setTerm(12);
        sampleContract.setRepaymentMethod(RepaymentMethod.EQUAL_INSTALLMENT);
        sampleContract.setStartDate(LocalDate.of(2026, 5, 1));

        sampleSchedule = new RepaymentSchedule();
        sampleSchedule.setId(1L);
        sampleSchedule.setContractId(1L);
        sampleSchedule.setContractNo("LOAN20260426001");
        sampleSchedule.setPrincipal(new BigDecimal("120000"));
        sampleSchedule.setAnnualRate(new BigDecimal("0.12"));
        sampleSchedule.setTerm(12);
        sampleSchedule.setTotalPeriods(12);
        sampleSchedule.setRepaymentMethod(RepaymentMethod.EQUAL_INSTALLMENT);
        sampleSchedule.setStartDate(LocalDate.of(2026, 5, 1));
        sampleSchedule.setStatus(ScheduleStatus.ACTIVE);

        samplePeriod = new RepaymentPeriod();
        samplePeriod.setId(1L);
        samplePeriod.setPeriodNo(1);
        samplePeriod.setDueDate(LocalDate.of(2026, 6, 1));
        samplePeriod.setTotalAmount(new BigDecimal("10661.85"));
        samplePeriod.setPrincipal(new BigDecimal("9661.85"));
        samplePeriod.setInterest(new BigDecimal("1000.00"));
        samplePeriod.setRemainingPrincipal(new BigDecimal("110338.15"));
        samplePeriod.setStatus(RepaymentPeriodStatus.PENDING);
    }

    @Nested
    @DisplayName("生成还款计划 generateSchedule")
    class GenerateSchedule {

        @Test
        @DisplayName("按合同ID生成应成功")
        void shouldGenerateByContractId() {
            given(contractRepository.findById(1L)).willReturn(Optional.of(sampleContract));
            given(scheduleRepository.save(any(RepaymentSchedule.class))).willAnswer(invocation -> {
                RepaymentSchedule s = invocation.getArgument(0);
                s.setId(1L);
                return s;
            });

            RepaymentSchedule result = scheduleService.generateSchedule(1L);

            assertThat(result).isNotNull();
            assertThat(result.getContractId()).isEqualTo(1L);
            assertThat(result.getTotalPeriods()).isEqualTo(12);
            assertThat(result.getPeriods()).hasSize(12);
            verify(scheduleRepository, times(1)).save(any(RepaymentSchedule.class));
        }

        @Test
        @DisplayName("合同不存在应抛出异常")
        void shouldThrowExceptionWhenContractNotFound() {
            given(contractRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> scheduleService.generateSchedule(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("合同不存在");
        }

        @Test
        @DisplayName("等额本息计算应正确")
        void shouldCalculateEqualInstallmentCorrectly() {
            given(scheduleRepository.save(any(RepaymentSchedule.class))).willAnswer(invocation -> {
                RepaymentSchedule s = invocation.getArgument(0);
                s.setId(1L);
                return s;
            });

            RepaymentSchedule result = scheduleService.generateSchedule(
                    1L, new BigDecimal("120000"), new BigDecimal("0.12"),
                    12, "EQUAL_INSTALLMENT", LocalDate.of(2026, 5, 1));

            assertThat(result.getPeriods()).hasSize(12);
            // 每期还款额应相同（最后1期因四舍五入修正可能略有差异）
            BigDecimal firstPayment = result.getPeriods().get(0).getTotalAmount();
            BigDecimal lastPayment = result.getPeriods().get(11).getTotalAmount();
            assertThat(firstPayment).isPositive();
            assertThat(lastPayment).isPositive();
            // 验证总还款额合理（本金+利息）
            BigDecimal totalPayment = result.getPeriods().stream()
                    .map(RepaymentPeriod::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(totalPayment).isGreaterThan(new BigDecimal("120000"));
        }

        @Test
        @DisplayName("等额本金计算应正确")
        void shouldCalculateEqualPrincipalCorrectly() {
            given(scheduleRepository.save(any(RepaymentSchedule.class))).willAnswer(invocation -> {
                RepaymentSchedule s = invocation.getArgument(0);
                s.setId(1L);
                return s;
            });

            RepaymentSchedule result = scheduleService.generateSchedule(
                    1L, new BigDecimal("120000"), new BigDecimal("0.12"),
                    12, "EQUAL_PRINCIPAL", LocalDate.of(2026, 5, 1));

            assertThat(result.getPeriods()).hasSize(12);
            // 每期本金应相同
            BigDecimal principalPerPeriod = result.getPeriods().get(0).getPrincipal();
            assertThat(principalPerPeriod).isEqualByComparingTo(new BigDecimal("10000.00"));
            // 利息逐期递减
            BigDecimal interest1 = result.getPeriods().get(0).getInterest();
            BigDecimal interest2 = result.getPeriods().get(1).getInterest();
            assertThat(interest1).isGreaterThan(interest2);
        }

        @Test
        @DisplayName("先息后本计算应正确")
        void shouldCalculateInterestOnlyCorrectly() {
            given(scheduleRepository.save(any(RepaymentSchedule.class))).willAnswer(invocation -> {
                RepaymentSchedule s = invocation.getArgument(0);
                s.setId(1L);
                return s;
            });

            RepaymentSchedule result = scheduleService.generateSchedule(
                    1L, new BigDecimal("120000"), new BigDecimal("0.12"),
                    12, "INTEREST_ONLY", LocalDate.of(2026, 5, 1));

            assertThat(result.getPeriods()).hasSize(12);
            // 前11期只还利息
            for (int i = 0; i < 11; i++) {
                assertThat(result.getPeriods().get(i).getPrincipal()).isEqualByComparingTo(BigDecimal.ZERO);
                assertThat(result.getPeriods().get(i).getInterest()).isPositive();
            }
            // 最后1期还本+息
            assertThat(result.getPeriods().get(11).getPrincipal()).isEqualByComparingTo(new BigDecimal("120000"));
        }

        @Test
        @DisplayName("气球贷计算应正确")
        void shouldCalculateBalloonCorrectly() {
            given(scheduleRepository.save(any(RepaymentSchedule.class))).willAnswer(invocation -> {
                RepaymentSchedule s = invocation.getArgument(0);
                s.setId(1L);
                return s;
            });

            RepaymentSchedule result = scheduleService.generateSchedule(
                    1L, new BigDecimal("120000"), new BigDecimal("0.12"),
                    12, "BALLOON", LocalDate.of(2026, 5, 1));

            assertThat(result.getPeriods()).hasSize(12);
            // 最后1期还剩余本金
            assertThat(result.getPeriods().get(11).getPrincipal()).isPositive();
        }

        @Test
        @DisplayName("到期一次性还本付息计算应正确")
        void shouldCalculateDueOneTimeCorrectly() {
            given(scheduleRepository.save(any(RepaymentSchedule.class))).willAnswer(invocation -> {
                RepaymentSchedule s = invocation.getArgument(0);
                s.setId(1L);
                return s;
            });

            RepaymentSchedule result = scheduleService.generateSchedule(
                    1L, new BigDecimal("120000"), new BigDecimal("0.12"),
                    12, "DUE_ONE_TIME", LocalDate.of(2026, 5, 1));

            assertThat(result.getPeriods()).hasSize(12);
            // 前11期金额为0
            for (int i = 0; i < 11; i++) {
                assertThat(result.getPeriods().get(i).getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            }
            // 最后1期还本+全部利息: 120000 + 120000*0.12*12/12 = 134400
            BigDecimal lastTotal = result.getPeriods().get(11).getTotalAmount();
            assertThat(lastTotal).isEqualByComparingTo(new BigDecimal("134400.00"));
        }

        @Test
        @DisplayName("不支持的还款方式应抛出异常")
        void shouldThrowExceptionForInvalidMethod() {
            assertThatThrownBy(() -> scheduleService.generateSchedule(
                    1L, new BigDecimal("120000"), new BigDecimal("0.12"),
                    12, "INVALID_METHOD", LocalDate.of(2026, 5, 1)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("不支持的还款方式");
        }
    }

    @Nested
    @DisplayName("查询还款计划")
    class GetSchedule {

        @Test
        @DisplayName("按合同ID查询应返回计划")
        void shouldReturnByContractId() {
            given(scheduleRepository.findByContractId(1L)).willReturn(Optional.of(sampleSchedule));

            Optional<RepaymentSchedule> result = scheduleService.getScheduleByContractId(1L);

            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("按合同ID查询含期次应返回计划")
        void shouldReturnByContractIdWithPeriods() {
            given(scheduleRepository.findByContractIdWithPeriods(1L)).willReturn(Optional.of(sampleSchedule));

            Optional<RepaymentSchedule> result = scheduleService.getScheduleByContractIdWithPeriods(1L);

            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("按计划ID查询期次应返回列表")
        void shouldReturnPeriodsByScheduleId() {
            given(periodRepository.findByScheduleIdOrderByPeriodNoAsc(1L))
                    .willReturn(Collections.singletonList(samplePeriod));

            List<RepaymentPeriod> result = scheduleService.getPeriodsByScheduleId(1L);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("查询当前期次应返回待还的第一期")
        void shouldReturnCurrentPeriod() {
            RepaymentPeriod paidPeriod = new RepaymentPeriod();
            paidPeriod.setPeriodNo(1);
            paidPeriod.setStatus(RepaymentPeriodStatus.PAID);

            RepaymentPeriod pendingPeriod = new RepaymentPeriod();
            pendingPeriod.setPeriodNo(2);
            pendingPeriod.setStatus(RepaymentPeriodStatus.PENDING);

            given(periodRepository.findByScheduleIdOrderByPeriodNoAsc(1L))
                    .willReturn(List.of(paidPeriod, pendingPeriod));

            RepaymentPeriod result = scheduleService.getCurrentPeriod(1L);

            assertThat(result).isNotNull();
            assertThat(result.getPeriodNo()).isEqualTo(2);
        }

        @Test
        @DisplayName("全部已还时当前期次应为null")
        void shouldReturnNullWhenAllPaid() {
            RepaymentPeriod paidPeriod = new RepaymentPeriod();
            paidPeriod.setPeriodNo(1);
            paidPeriod.setStatus(RepaymentPeriodStatus.PAID);

            given(periodRepository.findByScheduleIdOrderByPeriodNoAsc(1L))
                    .willReturn(Collections.singletonList(paidPeriod));

            RepaymentPeriod result = scheduleService.getCurrentPeriod(1L);

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("还款操作 makePayment")
    class MakePayment {

        @Test
        @DisplayName("全额还款应标记为已还")
        void shouldMarkAsPaidWhenFullPayment() {
            given(periodRepository.findById(1L)).willReturn(Optional.of(samplePeriod));
            given(periodRepository.findByScheduleIdOrderByPeriodNoAsc(1L))
                    .willReturn(Collections.singletonList(samplePeriod));
            given(periodRepository.save(any(RepaymentPeriod.class))).willAnswer(invocation -> invocation.getArgument(0));

            scheduleService.makePayment(1L, 1L, new BigDecimal("10661.85"));

            assertThat(samplePeriod.getStatus()).isEqualTo(RepaymentPeriodStatus.PAID);
            assertThat(samplePeriod.getPaidDate()).isNotNull();
        }

        @Test
        @DisplayName("部分还款应标记为部分还款")
        void shouldMarkAsPartialWhenPartialPayment() {
            given(periodRepository.findById(1L)).willReturn(Optional.of(samplePeriod));
            given(periodRepository.findByScheduleIdOrderByPeriodNoAsc(1L))
                    .willReturn(Collections.singletonList(samplePeriod));
            given(periodRepository.save(any(RepaymentPeriod.class))).willAnswer(invocation -> invocation.getArgument(0));

            scheduleService.makePayment(1L, 1L, new BigDecimal("5000.00"));

            assertThat(samplePeriod.getStatus()).isEqualTo(RepaymentPeriodStatus.PARTIAL);
        }

        @Test
        @DisplayName("已还款期次再次还款应抛出异常")
        void shouldThrowExceptionWhenAlreadyPaid() {
            samplePeriod.setStatus(RepaymentPeriodStatus.PAID);
            given(periodRepository.findById(1L)).willReturn(Optional.of(samplePeriod));

            assertThatThrownBy(() -> scheduleService.makePayment(1L, 1L, new BigDecimal("10661.85")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("已还款");
        }

        @Test
        @DisplayName("全部还清后计划状态应更新为已完成")
        void shouldCompleteScheduleWhenAllPaid() {
            given(periodRepository.findById(1L)).willReturn(Optional.of(samplePeriod));
            given(periodRepository.findByScheduleIdOrderByPeriodNoAsc(1L))
                    .willReturn(Collections.singletonList(samplePeriod));
            given(periodRepository.save(any(RepaymentPeriod.class))).willAnswer(invocation -> invocation.getArgument(0));
            given(scheduleRepository.findById(1L)).willReturn(Optional.of(sampleSchedule));

            scheduleService.makePayment(1L, 1L, new BigDecimal("10661.85"));

            assertThat(sampleSchedule.getStatus()).isEqualTo(ScheduleStatus.COMPLETED);
        }
    }

    @Nested
    @DisplayName("逾期处理 markOverduePeriods")
    class MarkOverdue {

        @Test
        @DisplayName("应标记逾期期次并计算罚息")
        void shouldMarkOverduePeriods() {
            RepaymentPeriod overduePeriod = new RepaymentPeriod();
            overduePeriod.setId(1L);
            overduePeriod.setTotalAmount(new BigDecimal("10661.85"));
            overduePeriod.setDueDate(LocalDate.now().minusDays(10));
            overduePeriod.setStatus(RepaymentPeriodStatus.PENDING);

            given(periodRepository.findOverduePeriods(any(LocalDate.class)))
                    .willReturn(Collections.singletonList(overduePeriod));
            given(periodRepository.save(any(RepaymentPeriod.class))).willAnswer(invocation -> invocation.getArgument(0));

            scheduleService.markOverduePeriods();

            assertThat(overduePeriod.getStatus()).isEqualTo(RepaymentPeriodStatus.OVERDUE);
            assertThat(overduePeriod.getOverdueDays()).isEqualTo(10);
            assertThat(overduePeriod.getOverdueFine()).isPositive();
        }
    }

    @Nested
    @DisplayName("修改还款计划 modifySchedule")
    class ModifySchedule {

        @Test
        @DisplayName("应成功修改还款计划")
        void shouldModifySuccessfully() {
            given(scheduleRepository.findById(1L)).willReturn(Optional.of(sampleSchedule));
            given(scheduleRepository.save(any(RepaymentSchedule.class))).willAnswer(invocation -> invocation.getArgument(0));

            scheduleService.modifySchedule(1L, 24, "展期", "ADMIN");

            assertThat(sampleSchedule.getModificationType()).isEqualTo("TERM_ADJUSTMENT");
            assertThat(sampleSchedule.getModificationReason()).isEqualTo("展期");
            verify(scheduleRepository, times(2)).save(any(RepaymentSchedule.class));
        }

        @Test
        @DisplayName("不存在的计划应抛出异常")
        void shouldThrowExceptionWhenNotFound() {
            given(scheduleRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> scheduleService.modifySchedule(999L, 24, "test", "ADMIN"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("还款计划不存在");
        }

        @Test
        @DisplayName("非活跃计划不能修改")
        void shouldNotModifyNonActive() {
            sampleSchedule.setStatus(ScheduleStatus.COMPLETED);
            given(scheduleRepository.findById(1L)).willReturn(Optional.of(sampleSchedule));

            assertThatThrownBy(() -> scheduleService.modifySchedule(1L, 24, "test", "ADMIN"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("仅活跃的还款计划可以修改");
        }
    }
}
