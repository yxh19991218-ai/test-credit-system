package com.credit.system.controller;

import com.credit.system.domain.LoanProduct;
import com.credit.system.domain.enums.ProductStatus;
import com.credit.system.dto.ApiResponse;
import com.credit.system.dto.LoanProductRequest;
import com.credit.system.dto.LoanProductResponse;
import com.credit.system.dto.mapper.LoanProductMapper;
import com.credit.system.service.LoanProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@Tag(name = "贷款产品配置", description = "贷款产品 CRUD、上下架、资格查询")
public class LoanProductController {

    @Autowired
    private LoanProductService productService;

    @Autowired
    private LoanProductMapper productMapper;

    @PostMapping
    @Operation(summary = "创建贷款产品")
    public ResponseEntity<ApiResponse<LoanProductResponse>> createProduct(
            @RequestBody LoanProductRequest request,
            @RequestParam(defaultValue = "SYSTEM") String operator) {
        LoanProduct saved = productService.createProduct(request.toEntity(), operator);
        return ResponseEntity.ok(ApiResponse.success("产品创建成功", productMapper.toDto(saved)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询产品")
    public ResponseEntity<ApiResponse<LoanProductResponse>> getProductById(@PathVariable Long id) {
        return productService.getProductById(id)
                .map(p -> ResponseEntity.ok(ApiResponse.success(productMapper.toDto(p))))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{productCode}")
    @Operation(summary = "根据产品代码查询")
    public ResponseEntity<ApiResponse<LoanProductResponse>> getProductByCode(
            @PathVariable String productCode) {
        return productService.getProductByCode(productCode)
                .map(p -> ResponseEntity.ok(ApiResponse.success(productMapper.toDto(p))))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "分页查询产品列表")
    public ResponseEntity<ApiResponse<Page<LoanProductResponse>>> listProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        ProductStatus ps = null;
        if (status != null) ps = ProductStatus.valueOf(status);
        Page<LoanProduct> p = productService.getProductList(name, code, ps, page, size);
        return ResponseEntity.ok(ApiResponse.success(p.map(productMapper::toDto)));
    }

    @GetMapping("/eligible/{customerId}")
    @Operation(summary = "查询客户可申请的产品")
    public ResponseEntity<ApiResponse<List<LoanProductResponse>>> getEligibleProducts(
            @PathVariable Long customerId) {
        List<LoanProduct> products = productService.getEligibleProductsForCustomer(customerId);
        List<LoanProductResponse> resp = products.stream()
                .map(productMapper::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新产品信息")
    public ResponseEntity<ApiResponse<LoanProductResponse>> updateProduct(
            @PathVariable Long id,
            @RequestBody LoanProductRequest request,
            @RequestParam(defaultValue = "SYSTEM") String operator) {
        LoanProduct updated = productService.updateProduct(id, request.toEntity(), operator);
        return ResponseEntity.ok(ApiResponse.success(productMapper.toDto(updated)));
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "发布产品（草稿→活跃）")
    public ResponseEntity<ApiResponse<String>> publishProduct(
            @PathVariable Long id,
            @RequestParam(defaultValue = "SYSTEM") String operator) {
        productService.publishProduct(id, operator);
        return ResponseEntity.ok(ApiResponse.success("产品发布成功"));
    }

    @PostMapping("/{id}/unpublish")
    @Operation(summary = "下架产品（禁用）")
    public ResponseEntity<ApiResponse<String>> unpublishProduct(
            @PathVariable Long id,
            @RequestParam String reason,
            @RequestParam(defaultValue = "SYSTEM") String operator) {
        productService.unpublishProduct(id, reason, operator);
        return ResponseEntity.ok(ApiResponse.success("产品已下架"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除产品（标记为已废弃）")
    public ResponseEntity<ApiResponse<String>> deleteProduct(
            @PathVariable Long id,
            @RequestParam(defaultValue = "SYSTEM") String operator) {
        productService.deleteProduct(id, operator);
        return ResponseEntity.ok(ApiResponse.success("产品已删除"));
    }
}
