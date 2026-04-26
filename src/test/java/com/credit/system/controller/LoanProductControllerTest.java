package com.credit.system.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import java.math.BigDecimal;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.credit.system.domain.LoanProduct;
import com.credit.system.domain.enums.ProductStatus;
import com.credit.system.dto.ApiResponse;
import com.credit.system.dto.LoanProductRequest;
import com.credit.system.dto.LoanProductResponse;
import com.credit.system.service.LoanProductService;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoanProductController 单元测试")
class LoanProductControllerTest {

    @Mock
    private LoanProductService productService;

    @InjectMocks
    private LoanProductController productController;

    private LoanProduct sampleProduct;

    @BeforeEach
    void setUp() {
        sampleProduct = new LoanProduct();
        sampleProduct.setId(1L);
        sampleProduct.setProductCode("P001");
        sampleProduct.setProductName("消费贷");
        sampleProduct.setInterestRate(new BigDecimal("0.12"));
        sampleProduct.setMinAmount(new BigDecimal("5000"));
        sampleProduct.setMaxAmount(new BigDecimal("200000"));
        sampleProduct.setMinTerm(3);
        sampleProduct.setMaxTerm(36);
        sampleProduct.setStatus(ProductStatus.DRAFT);
    }

    @Nested
    @DisplayName("POST /api/products")
    class CreateProduct {

        @Test
        @DisplayName("应成功创建并返回200")
        void shouldCreateSuccessfully() {
            given(productService.createProduct(any(LoanProduct.class), anyString()))
                    .willReturn(sampleProduct);

            LoanProductRequest request = new LoanProductRequest();
            request.setProductCode("P001");
            request.setProductName("消费贷");

            ResponseEntity<ApiResponse<LoanProductResponse>> response =
                    productController.createProduct(request, "ADMIN");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getCode()).isEqualTo(200);
            assertThat(response.getBody().getData().getProductName()).isEqualTo("消费贷");
        }
    }

    @Nested
    @DisplayName("GET /api/products/{id}")
    class GetProductById {

        @Test
        @DisplayName("应返回产品信息")
        void shouldReturnProduct() {
            given(productService.getProductById(1L)).willReturn(Optional.of(sampleProduct));

            ResponseEntity<ApiResponse<LoanProductResponse>> response =
                    productController.getProductById(1L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData().getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("不存在的产品应返回404")
        void shouldReturn404WhenNotFound() {
            given(productService.getProductById(999L)).willReturn(Optional.empty());

            ResponseEntity<ApiResponse<LoanProductResponse>> response =
                    productController.getProductById(999L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("GET /api/products/code/{code}")
    class GetProductByCode {

        @Test
        @DisplayName("应返回产品信息")
        void shouldReturnProduct() {
            given(productService.getProductByCode("P001")).willReturn(Optional.of(sampleProduct));

            ResponseEntity<ApiResponse<LoanProductResponse>> response =
                    productController.getProductByCode("P001");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("不存在的代码应返回404")
        void shouldReturn404WhenNotFound() {
            given(productService.getProductByCode("INVALID")).willReturn(Optional.empty());

            ResponseEntity<ApiResponse<LoanProductResponse>> response =
                    productController.getProductByCode("INVALID");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("GET /api/products （分页）")
    class ListProducts {

        @Test
        @DisplayName("应返回分页结果")
        void shouldReturnPagedResult() {
            Page<LoanProduct> page = new PageImpl<>(Collections.singletonList(sampleProduct));
            given(productService.getProductList(any(), any(), any(), anyInt(), anyInt()))
                    .willReturn(page);

            ResponseEntity<ApiResponse<Page<LoanProductResponse>>> response =
                    productController.listProducts(null, null, null, 0, 10);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData().getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("GET /api/products/eligible/{customerId}")
    class GetEligibleProducts {

        @Test
        @DisplayName("应返回客户可申请产品列表")
        void shouldReturnEligibleProducts() {
            given(productService.getEligibleProductsForCustomer(1L))
                    .willReturn(Collections.singletonList(sampleProduct));

            ResponseEntity<ApiResponse<List<LoanProductResponse>>> response =
                    productController.getEligibleProducts(1L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("PUT /api/products/{id}")
    class UpdateProduct {

        @Test
        @DisplayName("应成功更新并返回200")
        void shouldUpdateSuccessfully() {
            given(productService.updateProduct(anyLong(), any(LoanProduct.class), anyString()))
                    .willReturn(sampleProduct);

            LoanProductRequest request = new LoanProductRequest();

            ResponseEntity<ApiResponse<LoanProductResponse>> response =
                    productController.updateProduct(1L, request, "ADMIN");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Nested
    @DisplayName("POST /api/products/{id}/publish")
    class PublishProduct {

        @Test
        @DisplayName("应成功发布并返回200")
        void shouldPublishSuccessfully() {
            doNothing().when(productService).publishProduct(1L, "ADMIN");

            ResponseEntity<ApiResponse<String>> response =
                    productController.publishProduct(1L, "ADMIN");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getCode()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("POST /api/products/{id}/unpublish")
    class UnpublishProduct {

        @Test
        @DisplayName("应成功下架并返回200")
        void shouldUnpublishSuccessfully() {
            doNothing().when(productService).unpublishProduct(1L, "调整", "ADMIN");

            ResponseEntity<ApiResponse<String>> response =
                    productController.unpublishProduct(1L, "调整", "ADMIN");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Nested
    @DisplayName("DELETE /api/products/{id}")
    class DeleteProduct {

        @Test
        @DisplayName("应成功删除并返回200")
        void shouldDeleteSuccessfully() {
            doNothing().when(productService).deleteProduct(1L, "ADMIN");

            ResponseEntity<ApiResponse<String>> response =
                    productController.deleteProduct(1L, "ADMIN");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }
}
