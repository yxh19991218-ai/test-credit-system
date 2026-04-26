package com.credit.system.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import java.math.BigDecimal;
import java.time.LocalDate;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.credit.system.domain.RepaymentPeriod;
import com.credit.system.domain.RepaymentPeriodStatus;
import com.credit.system.domain.RepaymentSchedule;
import com.credit.system.domain.enums.RepaymentMethod;
import com.credit.system.domain.enums.ScheduleStatus;
import com.credit.system.dto.ApiResponse;
import com.credit.system.dto.RepaymentScheduleResponse;
import com.credit.system.exception.ResourceNotFoundException;
import com.credit.system.service.RepaymentScheduleService;

@ExtendWith(MockitoExtension.class)
@DisplayName("RepaymentScheduleController 单元测试")
class RepaymentScheduleControllerTest {

    @Mock
    private RepaymentScheduleService scheduleService;

    @InjectMocks
    private RepaymentScheduleController scheduleController;

    private RepaymentSchedule sampleSchedule;
    private RepaymentPeriod samplePeriod;

    @BeforeEach
    void setUp() {
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
    @DisplayName("POST /api/schedules/generate/{contractId}")
    class GenerateSchedule {

        @Test
        @DisplayName("应成功生成并返回200")
        void shouldGenerateSuccessfully() {
            given(scheduleService.generateSchedule(1L)).willReturn(sampleSchedule);

            ResponseEntity<ApiResponse<RepaymentScheduleResponse>> response =
                    scheduleController.generateSchedule(1L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getCode()).isEqualTo(200);
            assertThat(response.getBody().getMessage()).contains("成功");
        }
    }

    @Nested
    @DisplayName("GET /api/schedules/contract/{contractId}")
    class GetByContract {

        @Test
        @DisplayName("应返回还款计划")
        void shouldReturnSchedule() {
            given(scheduleService.getScheduleByContractIdWithPeriods(1L))
                    .willReturn(Optional.of(sampleSchedule));

            ResponseEntity<ApiResponse<RepaymentScheduleResponse>> response =
                    scheduleController.getByContract(1L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData().getContractId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("不存在的合同应返回404")
        void shouldReturn404WhenNotFound() {
            given(scheduleService.getScheduleByContractIdWithPeriods(999L))
                    .willReturn(Optional.empty());

            ResponseEntity<ApiResponse<RepaymentScheduleResponse>> response =
                    scheduleController.getByContract(999L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("GET /api/schedules/{scheduleId}")
    class GetSchedule {

        @Test
        @DisplayName("应返回还款计划详情")
        void shouldReturnSchedule() {
            given(scheduleService.getScheduleByContractIdWithPeriods(1L))
                    .willReturn(Optional.of(sampleSchedule));

            ResponseEntity<ApiResponse<RepaymentScheduleResponse>> response =
                    scheduleController.getSchedule(1L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("不存在的计划应抛出异常")
        void shouldThrowExceptionWhenNotFound() {
            given(scheduleService.getScheduleByContractIdWithPeriods(999L))
                    .willReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> scheduleController.getSchedule(999L));
        }
    }

    @Nested
    @DisplayName("GET /api/schedules/{scheduleId}/current-period")
    class GetCurrentPeriod {

        @Test
        @DisplayName("应返回当前应还期次")
        void shouldReturnCurrentPeriod() {
            given(scheduleService.getCurrentPeriod(1L)).willReturn(samplePeriod);

            ResponseEntity<ApiResponse<RepaymentScheduleResponse.PeriodResponse>> response =
                    scheduleController.getCurrentPeriod(1L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData().getPeriodNo()).isEqualTo(1);
        }

        @Test
        @DisplayName("无待还期次时应返回null")
        void shouldReturnNullWhenNoPending() {
            given(scheduleService.getCurrentPeriod(1L)).willReturn(null);

            ResponseEntity<ApiResponse<RepaymentScheduleResponse.PeriodResponse>> response =
                    scheduleController.getCurrentPeriod(1L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData()).isNull();
        }
    }

    @Nested
    @DisplayName("POST /api/schedules/{scheduleId}/periods/{periodId}/pay")
    class MakePayment {

        @Test
        @DisplayName("应成功还款并返回200")
        void shouldPaySuccessfully() {
            doNothing().when(scheduleService).makePayment(1L, 1L, new BigDecimal("10661.85"));

            ResponseEntity<ApiResponse<String>> response =
                    scheduleController.makePayment(1L, 1L, new BigDecimal("10661.85"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getCode()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("POST /api/schedules/mark-overdue")
    class MarkOverdue {

        @Test
        @DisplayName("应成功标记逾期并返回200")
        void shouldMarkOverdueSuccessfully() {
            doNothing().when(scheduleService).markOverduePeriods();

            ResponseEntity<ApiResponse<String>> response =
                    scheduleController.markOverdue();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getCode()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("POST /api/schedules/{scheduleId}/modify-term")
    class ModifyTerm {

        @Test
        @DisplayName("应成功修改期限并返回200")
        void shouldModifySuccessfully() {
            doNothing().when(scheduleService).modifySchedule(1L, 24, "展期", "ADMIN");

            ResponseEntity<ApiResponse<String>> response =
                    scheduleController.modifyTerm(1L, 24, "展期", "ADMIN");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Nested
    @DisplayName("GET /api/schedules/{scheduleId}/periods")
    class GetPeriods {

        @Test
        @DisplayName("应返回期次列表")
        void shouldReturnPeriods() {
            given(scheduleService.getPeriodsByScheduleId(1L))
                    .willReturn(Collections.singletonList(samplePeriod));

            ResponseEntity<ApiResponse<?>> response =
                    scheduleController.getPeriods(1L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }
}
