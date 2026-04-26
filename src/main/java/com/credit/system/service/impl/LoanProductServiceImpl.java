package com.credit.system.service.impl;

import com.credit.system.domain.LoanProduct;
import com.credit.system.domain.enums.ProductStatus;
import com.credit.system.exception.BusinessException;
import com.credit.system.exception.ResourceNotFoundException;
import com.credit.system.repository.LoanProductRepository;
import com.credit.system.service.LoanProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class LoanProductServiceImpl implements LoanProductService {

    @Autowired
    private LoanProductRepository productRepository;

    @Override
    public LoanProduct createProduct(LoanProduct product, String operator) {
        if (productRepository.existsByProductCode(product.getProductCode())) {
            throw new BusinessException("产品代码已存在: " + product.getProductCode());
        }

        validateProduct(product);

        product.setStatus(ProductStatus.DRAFT);
        product.setCreatedBy(operator);
        return productRepository.save(product);
    }

    @Override
    public LoanProduct updateProduct(Long id, LoanProduct details, String operator) {
        LoanProduct product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("产品不存在，ID: " + id));

        if (product.getStatus() == ProductStatus.RETIRED) {
            throw new BusinessException("已下架产品不能修改");
        }

        if (details.getProductName() != null) product.setProductName(details.getProductName());
        if (details.getProductDescription() != null) product.setProductDescription(details.getProductDescription());
        if (details.getInterestRate() != null) product.setInterestRate(details.getInterestRate());
        if (details.getMinAmount() != null) product.setMinAmount(details.getMinAmount());
        if (details.getMaxAmount() != null) product.setMaxAmount(details.getMaxAmount());
        if (details.getMinTerm() != null) product.setMinTerm(details.getMinTerm());
        if (details.getMaxTerm() != null) product.setMaxTerm(details.getMaxTerm());
        if (details.getMinAge() != null) product.setMinAge(details.getMinAge());
        if (details.getMaxAge() != null) product.setMaxAge(details.getMaxAge());
        if (details.getMinCreditScore() != null) product.setMinCreditScore(details.getMinCreditScore());
        if (details.getMinMonthlyIncome() != null) product.setMinMonthlyIncome(details.getMinMonthlyIncome());
        if (details.getServiceFeeRate() != null) product.setServiceFeeRate(details.getServiceFeeRate());
        if (details.getHandlingFee() != null) product.setHandlingFee(details.getHandlingFee());

        product.setUpdatedBy(operator);
        return productRepository.save(product);
    }

    @Override
    public Optional<LoanProduct> getProductById(Long id) {
        return productRepository.findById(id);
    }

    @Override
    public Optional<LoanProduct> getProductByCode(String productCode) {
        return productRepository.findByProductCode(productCode);
    }

    @Override
    public Page<LoanProduct> getProductList(String name, String code, ProductStatus status,
                                           int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        if (name != null) {
            return productRepository.findByProductNameContaining(name, pageRequest);
        }
        return productRepository.findAll(pageRequest);
    }

    @Override
    public List<LoanProduct> getEligibleProductsForCustomer(Long customerId) {
        return productRepository.findEligibleProductsForCustomer(customerId);
    }

    @Override
    public void publishProduct(Long id, String operator) {
        LoanProduct product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("产品不存在，ID: " + id));

        if (product.getStatus() != ProductStatus.DRAFT) {
            throw new BusinessException("只能发布草稿状态的产品");
        }

        product.setStatus(ProductStatus.ACTIVE);
        product.setPublishDate(LocalDateTime.now());
        product.setPublishedBy(operator);
        product.setUpdatedBy(operator);
        productRepository.save(product);
    }

    @Override
    public void unpublishProduct(Long id, String reason, String operator) {
        LoanProduct product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("产品不存在，ID: " + id));

        if (productRepository.hasActiveLoans(id)) {
            throw new BusinessException("产品存在活跃贷款合同，无法下架");
        }

        product.setStatus(ProductStatus.DISABLED);
        product.setUnpublishDate(LocalDateTime.now());
        product.setUnpublishReason(reason);
        product.setUnpublishedBy(operator);
        product.setUpdatedBy(operator);
        productRepository.save(product);
    }

    @Override
    public void deleteProduct(Long id, String operator) {
        LoanProduct product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("产品不存在，ID: " + id));

        if (product.getStatus() == ProductStatus.ACTIVE) {
            throw new BusinessException("活跃状态产品不能直接删除，请先下架");
        }

        product.setStatus(ProductStatus.RETIRED);
        product.setUpdatedBy(operator);
        productRepository.save(product);
    }

    private void validateProduct(LoanProduct product) {
        if (product.getMinAmount().compareTo(product.getMaxAmount()) > 0) {
            throw new BusinessException("最小金额不能大于最大金额");
        }
        if (product.getMinTerm() > product.getMaxTerm()) {
            throw new BusinessException("最短期限不能大于最长期限");
        }
        if (product.getMinAge() != null && product.getMaxAge() != null
                && product.getMinAge() > product.getMaxAge()) {
            throw new BusinessException("最小年龄不能大于最大年龄");
        }
    }
}
