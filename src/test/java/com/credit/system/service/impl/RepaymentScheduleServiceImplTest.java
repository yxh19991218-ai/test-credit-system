package com.credit.system.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import com.credit.system.service.calculator.RepaymentCalculator;
import com.credit.system.service.calculator.RepaymentCalculatorRegistry;

@ExtendWith(MockitoExtension.class)
@DisplayName("RepaymentScheduleServiceImpl 单元测试")
class RepaymentScheduleServiceImplTest {

    @Mock
    private RepaymentScheduleRepository scheduleRepository;

    @Mock
    private RepaymentPeriodRepository periodRepository;

    @Mock
    private LoanContractRepository contractRepository;

    @Mock
    private RepaymentCalculatorRegistry calculatorRegistry;

    @Mock
    private RepaymentCalculator mockCalculator;

    private RepaymentScheduleServiceImpl scheduleService;

    private LoanContract sampleContract;
    private RepaymentSchedule sampleSchedule;
    private List<RepaymentPeriod> samplePeriods;

    @BeforeEach
    void setUp() {
        scheduleService = new RepaymentScheduleServiceImpl(
                scheduleRepository, periodRepository, contractRepository, calculatorRegistry);

        sampleContract = new LoanContract();
        sampleContract.setId(1L);
        sampleContract.setContractNo("CN001");
        sampleContract.setTotalAmount(new BigDecimal("120000"));
        sampleContract.setInterestRate(new BigDecimal("0.12"));
        sampleContract.setTerm(12);
        sampleContract.setRepaymentMethod(RepaymentMethod.EQUAL_INSTALLMENT);
        sampleContract.setStartDate(LocalDate.of(2024, 1, 1));

        sampleSchedule = new RepaymentSchedule();
        sampleSchedule.setId(1L);
        sampleSchedule.setContractId(1L);
        sampleSchedule.setContractNo("CN001");
        sampleSchedule.setPrincipal(new BigDecimal("120000"));
        sampleSchedule.setAnnualRate(new BigDecimal("0.12"));
        sampleSchedule.setTerm(12);
        sampleSchedule.setTotalPeriods(12);
        sampleSchedule.setRepaymentMethod(RepaymentMethod.EQUAL_INSTALLMENT);
        sampleSchedule.setStartDate(LocalDate.of(2024, 1, 1));
        sampleSchedule.setStatus(ScheduleStatus.ACTIVE);

        samplePeriods = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            RepaymentPeriod period = new RepaymentPeriod();
            period.setId((long) i);
            period.setScheduleId(1L);
            period.setPeriodNo(i);
            period.setDueDate(LocalDate.of(2024, i, 1));
            period.setTotalAmount(new BigDecimal("10661.85"));
            period.setPrincipal(new BigDecimal("10000.00"));
            period.setInterest(new BigDecimal("661.85"));
            period.setRemainingPrincipal(new BigDecimal("120000").subtract(
                    new BigDecimal("10000").multiply(BigDecimal.valueOf(i))));
            period.setStatus(RepaymentPeriodStatus.PENDING);
            samplePeriods.add(period);
        }
    }

    @Nested
    @DisplayName("生成还款计划 generateSchedule")
    class GenerateSchedule {

        @Test
        @DisplayName("根据合同ID应成功生成还款计划")
        void shouldGenerateScheduleFromContract() {
            given(contractRepository.findById(1L)).willReturn(Optional.of(sampleContract));
            given(calculatorRegistry.getCalculator(RepaymentMethod.EQUAL_INSTALLMENT))
                    .willReturn(mockCalculator);
            given(mockCalculator.calculate(any(RepaymentPeriod.class), any(BigDecimal.class),
                    any(BigDecimal.class), anyInt(), anyInt(), any(BigDecimal.class)))
                    .willAnswer(invocation -> {
                        RepaymentPeriod period = invocation.getArgument(0);
                        period.setTotalAmount(new BigDecimal("10661.85"));
                        period.setPrincipal(new BigDecimal("10000.00"));
                        period.setInterest(new BigDecimal("661.85"));
                        return null;
                    });
            given(scheduleRepository.save(any(RepaymentSchedule.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            RepaymentSchedule result = scheduleService.generateSchedule(1L);

            assertThat(result).isNotNull();
            assertThat(result.getContractId()).isEqualTo(1L);
            assertThat(result.getTerm()).isEqualTo(12);
            assertThat(result.getTotalPeriods()).isEqualTo(12);
            assertThat(result.getStatus()).isEqualTo(ScheduleStatus.ACTIVE);
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
        @DisplayName("不支持的还款方式应抛出异常")
        void shouldThrowExceptionWhenInvalidMethod() {
            assertThatThrownBy(() -> scheduleService.generateSchedule(1L,
                    new BigDecimal("120000"), new BigDecimal("0.12"), 12,
                    "INVALID_METHOD", LocalDate.of(2024, 1, 1)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("不支持的还款方式");
        }
    }

    @Nested
    @DisplayName("查询还款计划")
    class GetSchedule {

        @Test
        @DisplayName("按合同ID查询应返回还款计划")
        void shouldReturnScheduleByContractId() {
            given(scheduleRepository.findByContractId(1L)).willReturn(Optional.of(sampleSchedule));

            Optional<RepaymentSchedule> result = scheduleService.getScheduleByContractId(1L);

            assertThat(result).isPresent();
            assertThat(result.get().getContractId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("按合同ID查询含期次应返回还款计划")
        void shouldReturnScheduleWithPeriods() {
            given(scheduleRepository.findByContractIdWithPeriods(1L))
                    .willReturn(Optional.of(sampleSchedule));

            Optional<RepaymentSchedule> result = scheduleService.getScheduleByContractIdWithPeriods(1L);

            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("按计划ID查询期次列表")
        void shouldReturnPeriodsByScheduleId() {
            given(periodRepository.findByScheduleIdOrderByPeriodNoAsc(1L))
                    .willReturn(samplePeriods);

            List<RepaymentPeriod> result = scheduleService.getPeriodsByScheduleId(1L);

            assertThat(result).hasSize(12);
        }

        @Test
        @DisplayName("查询当前期次应返回第一个待还款期次")
        void shouldReturnCurrentPeriod() {
            given(periodRepository.findByScheduleIdOrderByPeriodNoAsc(1L))
                    .willReturn(samplePeriods);

            RepaymentPeriod result = scheduleService.getCurrentPeriod(1L);

            assertThat(result).isNotNull();
            assertThat(result.getPeriodNo()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("还款 makePayment")
    class MakePayment {

        @Test
        @DisplayName("全额还款应标记为已还")
        void shouldMarkAsPaidWhenFullPayment() {
            RepaymentPeriod period = samplePeriods.get(0);
            given(periodRepository.findById(1L)).willReturn(Optional.of(period));
            given(periodRepository.save(any(RepaymentPeriod.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            scheduleService.makePayment(1L, 1L, new BigDecimal("10661.85"));

            assertThat(period.getStatus()).isEqualTo(RepaymentPeriodStatus.PAID);
            assertThat(period.getPaidAmount()).isEqualByComparingTo(new BigDecimal("10661.85"));
        }

        @Test
        @DisplayName("部分还款应标记为部分还款")
        void shouldMarkAsPartialWhenPartialPayment() {
            RepaymentPeriod period = samplePeriods.get(0);
            given(periodRepository.findById(1L)).willReturn(Optional.of(period));
            given(periodRepository.save(any(RepaymentPeriod.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            scheduleService.makePayment(1L, 1L, new BigDecimal("5000.00"));

            assertThat(period.getStatus()).isEqualTo(RepaymentPeriodStatus.PARTIAL);
        }

        @Test
        @DisplayName("已还款期次不能重复还款")
        void shouldNotPayAlreadyPaidPeriod() {
            RepaymentPeriod period = samplePeriods.get(0);
            period.setStatus(RepaymentPeriodStatus.PAID);
            given(periodRepository.findById(1L)).willReturn(Optional.of(period));

            assertThatThrownBy(() -> scheduleService.makePayment(1L, 1L, new BigDecimal("10661.85")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("该期次已还款");
        }
    }

    @Nested
    @DisplayName("标记逾期 markOverduePeriods")
    class MarkOverdue {

        @Test
        @DisplayName("应标记逾期期次")
        void shouldMarkOverduePeriods() {
            RepaymentPeriod overduePeriod = samplePeriods.get(0);
            overduePeriod.setDueDate(LocalDate.of(2024, 1, 1));
            given(periodRepository.findOverduePeriods(any(LocalDate.class)))
                    .willReturn(List.of(overduePeriod));
            given(periodRepository.save(any(RepaymentPeriod.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            scheduleService.markOverduePeriods();

            assertThat(overduePeriod.getStatus()).isEqualTo(RepaymentPeriodStatus.OVERDUE);
            assertThat(overduePeriod.getOverdueDays()).isGreaterThan(0);
            assertThat(overduePeriod.getOverdueFine()).isNotNull();
        }
    }

    @Nested
    @DisplayName("修改还款计划 modifySchedule")
    class ModifySchedule {

        @Test
        @DisplayName("应成功修改还款计划")
        void shouldModifyScheduleSuccessfully() {
            given(scheduleRepository.findById(1L)).willReturn(Optional.of(sampleSchedule));
            given(contractRepository.findById(1L)).willReturn(Optional.of(sampleContract));
            given(calculatorRegistry.getCalculator(RepaymentMethod.EQUAL_INSTALLMENT))
                    .willReturn(mockCalculator);
            given(mockCalculator.calculate(any(RepaymentPeriod.class), any(BigDecimal.class),
                    any(BigDecimal.class), anyInt(), anyInt(), any(BigDecimal.class)))
                    .willAnswer(invocation -> {
                        RepaymentPeriod period = invocation.getArgument(0);
                        period.setTotalAmount(new BigDecimal("10661.85"));
                        period.setPrincipal(new BigDecimal("10000.00"));
                        period.setInterest(new BigDecimal("661.85"));
                        return null;
                    });
            given(scheduleRepository.save(any(RepaymentSchedule.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            scheduleService.modifySchedule(1L, 24, "展期", "ADMIN");

            assertThat(sampleSchedule.getModificationType()).isEqualTo("TERM_ADJUSTMENT");
            assertThat(sampleSchedule.getModificationReason()).isEqualTo("展期");
            assertThat(sampleSchedule.getModifiedBy()).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("非活跃计划不能修改")
        void shouldNotModifyInactiveSchedule() {
            sampleSchedule.setStatus(ScheduleStatus.COMPLETED);
            given(scheduleRepository.findById(1L)).willReturn(Optional.of(sampleSchedule));

            assertThatThrownBy(() -> scheduleService.modifySchedule(1L, 24, "展期", "ADMIN"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("仅活跃的还款计划可以修改");
        }
    }
}
