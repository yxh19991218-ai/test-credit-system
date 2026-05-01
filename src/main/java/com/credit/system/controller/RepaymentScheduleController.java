package com.credit.system.controller;

import com.credit.system.domain.RepaymentPeriod;
import com.credit.system.domain.RepaymentSchedule;
import com.credit.system.dto.ApiResponse;
import com.credit.system.dto.RepaymentScheduleResponse;
import com.credit.system.dto.mapper.RepaymentScheduleMapper;
import com.credit.system.exception.ResourceNotFoundException;
import com.credit.system.service.RepaymentScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Optional;

@RestController
@RequestMapping("/api/schedules")
@Tag(name = "还款计划", description = "还款计划生成、查询、还款、逾期标记")
public class RepaymentScheduleController {

    @Autowired
    private RepaymentScheduleService scheduleService;

    @Autowired
    private RepaymentScheduleMapper scheduleMapper;

    @PostMapping("/generate/{contractId}")
    @Operation(summary = "为合同生成还款计划")
    public ResponseEntity<ApiResponse<RepaymentScheduleResponse>> generateSchedule(
            @PathVariable Long contractId) {
        RepaymentSchedule schedule = scheduleService.generateSchedule(contractId);
        return ResponseEntity.ok(ApiResponse.success("还款计划生成成功",
                scheduleMapper.toDto(schedule)));
    }

    @GetMapping("/contract/{contractId}")
    @Operation(summary = "查询合同对应的还款计划（含分期明细）")
    public ResponseEntity<ApiResponse<RepaymentScheduleResponse>> getByContract(
            @PathVariable Long contractId) {
        Optional<RepaymentSchedule> schedule =
                scheduleService.getScheduleByContractIdWithPeriods(contractId);
        return schedule
                .map(s -> ResponseEntity.ok(ApiResponse.success(scheduleMapper.toDto(s))))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{scheduleId}")
    @Operation(summary = "查询还款计划详情")
    public ResponseEntity<ApiResponse<RepaymentScheduleResponse>> getSchedule(
            @PathVariable Long scheduleId) {
        RepaymentSchedule schedule = scheduleService.getScheduleByContractIdWithPeriods(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("还款计划不存在"));
        return ResponseEntity.ok(ApiResponse.success(scheduleMapper.toDto(schedule)));
    }

    @GetMapping("/{scheduleId}/current-period")
    @Operation(summary = "获取当前应还期次")
    public ResponseEntity<ApiResponse<RepaymentScheduleResponse.PeriodResponse>> getCurrentPeriod(
            @PathVariable Long scheduleId) {
        RepaymentPeriod period = scheduleService.getCurrentPeriod(scheduleId);
        if (period == null) {
            return ResponseEntity.ok(ApiResponse.success(null));
        }
        return ResponseEntity.ok(ApiResponse.success(
                scheduleMapper.toPeriodDto(period)));
    }

    @PostMapping("/{scheduleId}/periods/{periodId}/pay")
    @Operation(summary = "偿还指定期次的还款")
    public ResponseEntity<ApiResponse<String>> makePayment(
            @PathVariable Long scheduleId,
            @PathVariable Long periodId,
            @RequestParam BigDecimal amount) {
        scheduleService.makePayment(scheduleId, periodId, amount);
        return ResponseEntity.ok(ApiResponse.success("还款成功"));
    }

    @PostMapping("/mark-overdue")
    @Operation(summary = "标记逾期期次（定时任务触发）")
    public ResponseEntity<ApiResponse<String>> markOverdue() {
        scheduleService.markOverduePeriods();
        return ResponseEntity.ok(ApiResponse.success("逾期标记完成"));
    }

    @PostMapping("/{scheduleId}/modify-term")
    @Operation(summary = "修改还款计划期限（展期后调整计划）")
    public ResponseEntity<ApiResponse<String>> modifyTerm(
            @PathVariable Long scheduleId,
            @RequestParam int newTerm,
            @RequestParam String reason,
            @RequestParam(defaultValue = "SYSTEM") String operator) {
        scheduleService.modifySchedule(scheduleId, newTerm, reason, operator);
        return ResponseEntity.ok(ApiResponse.success("还款计划已调整"));
    }

    @GetMapping("/{scheduleId}/periods")
    @Operation(summary = "查询还款计划的所有期次")
    public ResponseEntity<ApiResponse<?>> getPeriods(@PathVariable Long scheduleId) {
        return ResponseEntity.ok(ApiResponse.success(
                scheduleService.getPeriodsByScheduleId(scheduleId).stream()
                        .map(scheduleMapper::toPeriodDto)
                        .collect(java.util.stream.Collectors.toList())));
    }
}
