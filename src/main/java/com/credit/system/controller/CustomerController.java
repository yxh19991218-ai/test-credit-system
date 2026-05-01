package com.credit.system.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.credit.system.domain.Customer;
import com.credit.system.domain.enums.CustomerStatus;
import com.credit.system.domain.enums.RiskLevel;
import com.credit.system.dto.ApiResponse;
import com.credit.system.dto.BatchStatusRequest;
import com.credit.system.dto.CustomerCreditRequest;
import com.credit.system.dto.CustomerRequest;
import com.credit.system.dto.CustomerResponse;
import com.credit.system.dto.CustomerStatusRequest;
import com.credit.system.dto.PageRequestDTO;
import com.credit.system.dto.mapper.CustomerMapper;
import com.credit.system.exception.ResourceNotFoundException;
import com.credit.system.service.CustomerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/customers")
@Tag(name = "客户管理", description = "客户信息CRUD、状态管理、征信管理")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerMapper customerMapper;

    @PostMapping
    @Operation(summary = "创建客户")
    public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(@Valid @RequestBody CustomerRequest request) {
        Customer customer = request.toEntity();
        Customer saved = customerService.createCustomer(customer, null);
        return ResponseEntity.ok(ApiResponse.success("客户创建成功", customerMapper.toDto(saved)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询客户")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomerById(@PathVariable Long id) {
        return customerService.getCustomerById(id)
                .map(c -> ResponseEntity.ok(ApiResponse.success(customerMapper.toDto(c))))
                .orElseThrow(() -> new ResourceNotFoundException("客户不存在，ID: " + id));
    }

    @GetMapping("/id-card/{idCard}")
    @Operation(summary = "根据身份证号查询客户")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomerByIdCard(@PathVariable String idCard) {
        return customerService.getCustomerByIdCard(idCard)
                .map(c -> ResponseEntity.ok(ApiResponse.success(customerMapper.toDto(c))))
                .orElseThrow(() -> new ResourceNotFoundException("未找到身份证号对应的客户: " + idCard));
    }

    @GetMapping("/phone/{phone}")
    @Operation(summary = "根据手机号查询客户")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomerByPhone(@PathVariable String phone) {
        return customerService.getCustomerByPhone(phone)
                .map(c -> ResponseEntity.ok(ApiResponse.success(customerMapper.toDto(c))))
                .orElseThrow(() -> new ResourceNotFoundException("未找到手机号对应的客户: " + phone));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新客户信息")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CustomerRequest request,
            @RequestParam(defaultValue = "SYSTEM") String operator) {
        Customer customer = request.toEntity();
        Customer updated = customerService.updateCustomer(id, customer, operator);
        return ResponseEntity.ok(ApiResponse.success("客户信息更新成功", customerMapper.toDto(updated)));
    }

    @GetMapping
    @Operation(summary = "分页查询客户列表")
    public ResponseEntity<ApiResponse<Page<CustomerResponse>>> listCustomers(PageRequestDTO pageRequest) {
        CustomerStatus status = null;
        RiskLevel riskLevel = null;
        try { if (pageRequest.getStatus() != null) status = CustomerStatus.valueOf(pageRequest.getStatus()); } catch (Exception ignored) {}
        try { if (pageRequest.getRiskLevel() != null) riskLevel = RiskLevel.valueOf(pageRequest.getRiskLevel()); } catch (Exception ignored) {}

        Page<Customer> page = customerService.getCustomerList(
                pageRequest.getName(), pageRequest.getPhone(), pageRequest.getIdCard(),
                pageRequest.getKeyword(),
                status, riskLevel, pageRequest.getPage(), pageRequest.getSize());
        Page<CustomerResponse> respPage = page.map(customerMapper::toDto);
        return ResponseEntity.ok(ApiResponse.success(respPage));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "更新客户状态")
    public ResponseEntity<ApiResponse<String>> updateCustomerStatus(
            @PathVariable Long id,
            @RequestBody CustomerStatusRequest request) {
        CustomerStatus status = CustomerStatus.valueOf(request.getStatus());
        customerService.updateCustomerStatus(id, status, request.getReason(), request.getOperator());
        return ResponseEntity.ok(ApiResponse.success("状态更新成功"));
    }

    @PatchMapping("/{id}/credit")
    @Operation(summary = "更新客户征信信息")
    public ResponseEntity<ApiResponse<String>> updateCreditInfo(
            @PathVariable Long id,
            @RequestBody CustomerCreditRequest request) {
        customerService.updateCreditInfo(id, request.getCreditScore(), request.getCreditReportNo());
        return ResponseEntity.ok(ApiResponse.success("征信信息更新成功"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除客户（软删除）")
    public ResponseEntity<ApiResponse<String>> deleteCustomer(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "手动删除") String reason,
            @RequestParam(defaultValue = "SYSTEM") String operator) {
        customerService.deleteCustomer(id, reason, operator);
        return ResponseEntity.ok(ApiResponse.success("客户已删除"));
    }

    @PostMapping("/batch-status")
    @Operation(summary = "批量更新客户状态")
    public ResponseEntity<ApiResponse<String>> batchUpdateStatus(
            @RequestBody BatchStatusRequest request) {
        CustomerStatus status = CustomerStatus.valueOf(request.getStatus());
        customerService.batchUpdateCustomerStatus(request.getCustomerIds(), status, request.getReason(), request.getOperator());
        return ResponseEntity.ok(ApiResponse.success("批量状态更新完成"));
    }
}
