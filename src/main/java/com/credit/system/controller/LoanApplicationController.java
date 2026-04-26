package com.credit.system.controller;

import com.credit.system.domain.LoanApplication;
import com.credit.system.domain.enums.ApplicationStatus;
import com.credit.system.dto.ApiResponse;
import com.credit.system.dto.ApplicationReviewRequest;
import com.credit.system.dto.LoanApplicationRequest;
import com.credit.system.dto.LoanApplicationResponse;
import com.credit.system.exception.ResourceNotFoundException;
import com.credit.system.service.LoanApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/applications")
@Tag(name = "贷款申请流程", description = "贷款申请提交、审批流转、额度核定")
public class LoanApplicationController {

    @Autowired
    private LoanApplicationService applicationService;

    @PostMapping
    @Operation(summary = "创建贷款申请")
    public ResponseEntity<ApiResponse<LoanApplicationResponse>> createApplication(
            @RequestBody LoanApplicationRequest request,
            @RequestParam(defaultValue = "SYSTEM") String operator) {
        LoanApplication saved = applicationService.createApplication(request.toEntity(), operator);
        return ResponseEntity.ok(ApiResponse.success("申请创建成功", LoanApplicationResponse.from(saved)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询申请")
    public ResponseEntity<ApiResponse<LoanApplicationResponse>> getApplication(@PathVariable Long id) {
        return applicationService.getApplicationById(id)
                .map(a -> ResponseEntity.ok(ApiResponse.success(LoanApplicationResponse.from(a))))
                .orElseThrow(() -> new ResourceNotFoundException("申请不存在，ID: " + id));
    }

    @GetMapping("/by-contract/{contractId}")
    @Operation(summary = "根据合同ID查询申请")
    public ResponseEntity<ApiResponse<LoanApplicationResponse>> getByContract(
            @PathVariable Long contractId) {
        return applicationService.getApplicationByContractId(contractId)
                .map(a -> ResponseEntity.ok(ApiResponse.success(LoanApplicationResponse.from(a))))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "查询申请列表")
    public ResponseEntity<ApiResponse<Page<LoanApplicationResponse>>> listApplications(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        ApplicationStatus s = null;
        if (status != null) s = ApplicationStatus.valueOf(status);
        Page<LoanApplication> result = applicationService.getApplicationList(
                customerId, productId, s, page, size);
        return ResponseEntity.ok(ApiResponse.success(result.map(LoanApplicationResponse::from)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改申请（仅草稿状态）")
    public ResponseEntity<ApiResponse<LoanApplicationResponse>> updateApplication(
            @PathVariable Long id,
            @RequestBody LoanApplicationRequest request) {
        LoanApplication updated = applicationService.updateApplication(id, request.toEntity());
        return ResponseEntity.ok(ApiResponse.success(LoanApplicationResponse.from(updated)));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交申请草稿审批")
    public ResponseEntity<ApiResponse<String>> submitApplication(@PathVariable Long id) {
        applicationService.submitApplication(id);
        return ResponseEntity.ok(ApiResponse.success("申请已提交"));
    }

    @PostMapping("/{id}/review")
    @Operation(summary = "审核贷款申请（审批/驳回）")
    public ResponseEntity<ApiResponse<String>> reviewApplication(
            @PathVariable Long id,
            @RequestBody ApplicationReviewRequest request) {
        ApplicationStatus decision = ApplicationStatus.valueOf(request.getDecision());
        applicationService.reviewApplication(id, decision, request.getReviewer(),
                request.getComments(), request.getApprovedAmount(),
                request.getApprovedTerm(), request.getInterestRate());
        return ResponseEntity.ok(ApiResponse.success("审核完成"));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消申请")
    public ResponseEntity<ApiResponse<String>> cancelApplication(
            @PathVariable Long id,
            @RequestParam String reason) {
        applicationService.cancelApplication(id, reason);
        return ResponseEntity.ok(ApiResponse.success("申请已取消"));
    }

    @PostMapping("/{id}/to-contract/{contractId}")
    @Operation(summary = "审批通过后生成合同（完成申请流程）")
    public ResponseEntity<ApiResponse<String>> approveToContract(
            @PathVariable Long id,
            @PathVariable Long contractId) {
        applicationService.approveToContract(id, contractId);
        return ResponseEntity.ok(ApiResponse.success("申请已完成并关联合同"));
    }
}
