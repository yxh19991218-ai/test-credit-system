package com.credit.system.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import org.springframework.data.domain.PageRequest;
import com.credit.system.domain.LoanProduct;
import com.credit.system.domain.enums.ProductStatus;
import com.credit.system.exception.BusinessException;
import com.credit.system.exception.ResourceNotFoundException;
import com.credit.system.repository.LoanProductRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoanProductServiceImpl 单元测试")
class LoanProductServiceImplTest {

    @Mock
    private LoanProductRepository productRepository;

    @InjectMocks
    private LoanProductServiceImpl productService;

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
        sampleProduct.setMinAge(18);
        sampleProduct.setMaxAge(65);
        sampleProduct.setStatus(ProductStatus.DRAFT);
    }

    @Nested
    @DisplayName("创建产品 createProduct")
    class CreateProduct {

        @Test
        @DisplayName("应成功创建产品")
        void shouldCreateSuccessfully() {
            given(productRepository.existsByProductCode("P001")).willReturn(false);
            given(productRepository.save(any(LoanProduct.class))).willReturn(sampleProduct);

            LoanProduct result = productService.createProduct(sampleProduct, "ADMIN");

            assertThat(result).isNotNull();
            assertThat(result.getProductCode()).isEqualTo("P001");
            assertThat(result.getStatus()).isEqualTo(ProductStatus.DRAFT);
            verify(productRepository, times(1)).save(any(LoanProduct.class));
        }

        @Test
        @DisplayName("产品代码重复应抛出异常")
        void shouldThrowExceptionWhenCodeExists() {
            given(productRepository.existsByProductCode("P001")).willReturn(true);

            assertThatThrownBy(() -> productService.createProduct(sampleProduct, "ADMIN"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("产品代码已存在");

            verify(productRepository, never()).save(any());
        }

        @Test
        @DisplayName("最小金额大于最大金额应抛出异常")
        void shouldThrowExceptionWhenMinAmountGreaterThanMax() {
            sampleProduct.setMinAmount(new BigDecimal("300000"));
            sampleProduct.setMaxAmount(new BigDecimal("100000"));
            given(productRepository.existsByProductCode("P001")).willReturn(false);

            assertThatThrownBy(() -> productService.createProduct(sampleProduct, "ADMIN"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("最小金额不能大于最大金额");
        }

        @Test
        @DisplayName("最短期限大于最长期限应抛出异常")
        void shouldThrowExceptionWhenMinTermGreaterThanMax() {
            sampleProduct.setMinTerm(36);
            sampleProduct.setMaxTerm(3);
            given(productRepository.existsByProductCode("P001")).willReturn(false);

            assertThatThrownBy(() -> productService.createProduct(sampleProduct, "ADMIN"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("最短期限不能大于最长期限");
        }
    }

    @Nested
    @DisplayName("更新产品 updateProduct")
    class UpdateProduct {

        @Test
        @DisplayName("应成功更新产品信息")
        void shouldUpdateSuccessfully() {
            given(productRepository.findById(1L)).willReturn(Optional.of(sampleProduct));
            given(productRepository.save(any(LoanProduct.class))).willAnswer(invocation -> invocation.getArgument(0));

            LoanProduct details = new LoanProduct();
            details.setProductName("消费贷(升级版)");
            details.setInterestRate(new BigDecimal("0.15"));

            LoanProduct result = productService.updateProduct(1L, details, "ADMIN");

            assertThat(result.getProductName()).isEqualTo("消费贷(升级版)");
            assertThat(result.getInterestRate()).isEqualByComparingTo(new BigDecimal("0.15"));
        }

        @Test
        @DisplayName("更新不存在的产品应抛出异常")
        void shouldThrowExceptionWhenNotFound() {
            given(productRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> productService.updateProduct(999L, new LoanProduct(), "ADMIN"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("产品不存在");
        }

        @Test
        @DisplayName("已下架产品不能修改")
        void shouldNotUpdateRetiredProduct() {
            sampleProduct.setStatus(ProductStatus.RETIRED);
            given(productRepository.findById(1L)).willReturn(Optional.of(sampleProduct));

            assertThatThrownBy(() -> productService.updateProduct(1L, new LoanProduct(), "ADMIN"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("已下架产品不能修改");
        }
    }

    @Nested
    @DisplayName("查询产品")
    class GetProduct {

        @Test
        @DisplayName("按ID查询应返回产品")
        void shouldReturnProductById() {
            given(productRepository.findById(1L)).willReturn(Optional.of(sampleProduct));

            Optional<LoanProduct> result = productService.getProductById(1L);

            assertThat(result).isPresent();
            assertThat(result.get().getProductName()).isEqualTo("消费贷");
        }

        @Test
        @DisplayName("按代码查询应返回产品")
        void shouldReturnProductByCode() {
            given(productRepository.findByProductCode("P001")).willReturn(Optional.of(sampleProduct));

            Optional<LoanProduct> result = productService.getProductByCode("P001");

            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("分页查询应返回结果")
        void shouldReturnPagedResults() {
            Page<LoanProduct> page = new PageImpl<>(Collections.singletonList(sampleProduct));
            given(productRepository.findAll(any(PageRequest.class))).willReturn(page);

            Page<LoanProduct> result = productService.getProductList(null, null, null, 0, 10);

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("按名称模糊查询应返回结果")
        void shouldFindByNameContaining() {
            Page<LoanProduct> page = new PageImpl<>(Collections.singletonList(sampleProduct));
            given(productRepository.findByProductNameContaining(eq("消费"), any(PageRequest.class))).willReturn(page);

            Page<LoanProduct> result = productService.getProductList("消费", null, null, 0, 10);

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("查询客户可申请产品")
        void shouldReturnEligibleProducts() {
            given(productRepository.findEligibleProductsForCustomer(1L))
                    .willReturn(Collections.singletonList(sampleProduct));

            List<LoanProduct> result = productService.getEligibleProductsForCustomer(1L);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("产品上下架")
    class PublishUnpublish {

        @Test
        @DisplayName("发布草稿产品应成功")
        void shouldPublishDraftProduct() {
            given(productRepository.findById(1L)).willReturn(Optional.of(sampleProduct));
            given(productRepository.save(any(LoanProduct.class))).willAnswer(invocation -> invocation.getArgument(0));

            productService.publishProduct(1L, "ADMIN");

            assertThat(sampleProduct.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        }

        @Test
        @DisplayName("非草稿产品发布应抛出异常")
        void shouldNotPublishNonDraftProduct() {
            sampleProduct.setStatus(ProductStatus.ACTIVE);
            given(productRepository.findById(1L)).willReturn(Optional.of(sampleProduct));

            assertThatThrownBy(() -> productService.publishProduct(1L, "ADMIN"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("只能发布草稿状态");
        }

        @Test
        @DisplayName("下架产品应成功")
        void shouldUnpublishSuccessfully() {
            sampleProduct.setStatus(ProductStatus.ACTIVE);
            given(productRepository.findById(1L)).willReturn(Optional.of(sampleProduct));
            given(productRepository.hasActiveLoans(1L)).willReturn(false);
            given(productRepository.save(any(LoanProduct.class))).willAnswer(invocation -> invocation.getArgument(0));

            productService.unpublishProduct(1L, "产品调整", "ADMIN");

            assertThat(sampleProduct.getStatus()).isEqualTo(ProductStatus.DISABLED);
        }

        @Test
        @DisplayName("存在活跃贷款时下架应抛出异常")
        void shouldNotUnpublishWithActiveLoans() {
            sampleProduct.setStatus(ProductStatus.ACTIVE);
            given(productRepository.findById(1L)).willReturn(Optional.of(sampleProduct));
            given(productRepository.hasActiveLoans(1L)).willReturn(true);

            assertThatThrownBy(() -> productService.unpublishProduct(1L, "test", "ADMIN"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("存在活跃贷款合同");
        }
    }

    @Nested
    @DisplayName("删除产品 deleteProduct")
    class DeleteProduct {

        @Test
        @DisplayName("删除草稿产品应成功")
        void shouldDeleteDraftProduct() {
            given(productRepository.findById(1L)).willReturn(Optional.of(sampleProduct));
            given(productRepository.save(any(LoanProduct.class))).willAnswer(invocation -> invocation.getArgument(0));

            productService.deleteProduct(1L, "ADMIN");

            assertThat(sampleProduct.getStatus()).isEqualTo(ProductStatus.RETIRED);
        }

        @Test
        @DisplayName("活跃产品不能删除")
        void shouldNotDeleteActiveProduct() {
            sampleProduct.setStatus(ProductStatus.ACTIVE);
            given(productRepository.findById(1L)).willReturn(Optional.of(sampleProduct));

            assertThatThrownBy(() -> productService.deleteProduct(1L, "ADMIN"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("不能直接删除");
        }
    }
}
