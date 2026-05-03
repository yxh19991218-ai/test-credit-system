package com.credit.system.controller;

import com.credit.system.domain.LoanContract;
import com.credit.system.domain.enums.ContractStatus;
import com.credit.system.dto.ApiResponse;
import com.credit.system.dto.ContractRequest;
import com.credit.system.dto.ContractResponse;
import com.credit.system.dto.InterestRateChangeRequest;
import com.credit.system.dto.mapper.ContractMapper;
import com.credit.system.exception.ResourceNotFoundException;
import com.credit.system.service.ContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/contracts")
@Tag(name = "合同管理", description = "合同生成、签署、展期、终止、结清")
public class ContractController {

    @Autowired
    private ContractService contractService;

    @Autowired
    private ContractMapper contractMapper;

    @PostMapping
    @Operation(summary = "创建贷款合同")
    public ResponseEntity<ApiResponse<ContractResponse>> createContract(
            @RequestBody ContractRequest request,
            @RequestParam(defaultValue = "SYSTEM") String operator) {
        LoanContract saved = contractService.createContract(request.toEntity(), operator);
        return ResponseEntity.ok(ApiResponse.success("合同创建成功", contractMapper.toDto(saved)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询合同")
    public ResponseEntity<ApiResponse<ContractResponse>> getContract(@PathVariable Long id) {
        return contractService.getContractById(id)
                .map(c -> ResponseEntity.ok(ApiResponse.success(contractMapper.toDto(c))))
                .orElseThrow(() -> new ResourceNotFoundException("合同不存在，ID: " + id));
    }

    @GetMapping("/no/{contractNo}")
    @Operation(summary = "根据合同号查询")
    public ResponseEntity<ApiResponse<ContractResponse>> getByNo(@PathVariable String contractNo) {
        return contractService.getContractByNo(contractNo)
                .map(c -> ResponseEntity.ok(ApiResponse.success(contractMapper.toDto(c))))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-application/{applicationId}")
    @Operation(summary = "根据申请ID查询合同")
    public ResponseEntity<ApiResponse<ContractResponse>> getByApplication(
            @PathVariable Long applicationId) {
        return contractService.getContractByApplicationId(applicationId)
                .map(c -> ResponseEntity.ok(ApiResponse.success(contractMapper.toDto(c))))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "查询客户所有合同")
    public ResponseEntity<ApiResponse<List<ContractResponse>>> getByCustomer(
            @PathVariable Long customerId) {
        List<ContractResponse> list = contractService.getContractsByCustomerId(customerId)
                .stream().map(contractMapper::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping
    @Operation(summary = "分页查询合同列表")
    public ResponseEntity<ApiResponse<Page<ContractResponse>>> listContracts(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        ContractStatus s = null;
        if (status != null) s = ContractStatus.valueOf(status);
        Page<LoanContract> result = contractService.getContractList(
                customerId, productId, s, page, size);
        return ResponseEntity.ok(ApiResponse.success(result.map(contractMapper::toDto)));
    }

    @PostMapping("/{id}/sign")
    @Operation(summary = "签署合同")
    public ResponseEntity<ApiResponse<String>> signContract(
            @PathVariable Long id,
            @RequestParam String signatory,
            @RequestParam(defaultValue = "ONLINE") String signatureMethod) {
        contractService.signContract(id, signatory, signatureMethod);
        return ResponseEntity.ok(ApiResponse.success("合同签署成功"));
    }

    @PostMapping("/{id}/terminate")
    @Operation(summary = "终止合同")
    public ResponseEntity<ApiResponse<String>> terminateContract(
            @PathVariable Long id,
            @RequestParam String reason,
            @RequestParam(defaultValue = "SYSTEM") String operator) {
        contractService.terminateContract(id, reason, operator);
        return ResponseEntity.ok(ApiResponse.success("合同已终止"));
    }

    @PostMapping("/{id}/extend")
    @Operation(summary = "合同展期")
    public ResponseEntity<ApiResponse<String>> extendContract(
            @PathVariable Long id,
            @RequestParam int months,
            @RequestParam String reason,
            @RequestParam(defaultValue = "SYSTEM") String operator) {
        contractService.extendContract(id, months, reason, operator);
        return ResponseEntity.ok(ApiResponse.success("合同已展期"));
    }

    @PostMapping("/{id}/settle")
    @Operation(summary = "结清合同")
    public ResponseEntity<ApiResponse<String>> settleContract(@PathVariable Long id) {
        contractService.settleContract(id);
        return ResponseEntity.ok(ApiResponse.success("合同已结清"));
    }

    @PostMapping("/{id}/change-interest-rate")
    @Operation(summary = "变更合同利率")
    public ResponseEntity<ApiResponse<String>> changeInterestRate(
            @PathVariable Long id,
            @RequestBody @Valid InterestRateChangeRequest request,
            @RequestParam(defaultValue = "SYSTEM") String operator) {
        contractService.changeInterestRate(id, request, operator);
        return ResponseEntity.ok(ApiResponse.success("利率变更成功"));
    }

    @GetMapping("/overdue")
    @Operation(summary = "查询逾期合同列表")
    public ResponseEntity<ApiResponse<List<ContractResponse>>> getOverdue() {
        List<ContractResponse> list = contractService.getOverdueContracts()
                .stream().map(contractMapper::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/due-between")
    @Operation(summary = "查询指定日期范围内到期的合同")
    public ResponseEntity<ApiResponse<List<ContractResponse>>> getDueBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<ContractResponse> list = contractService.getContractsDueBetween(from, to)
                .stream().map(contractMapper::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(list));
    }
}
