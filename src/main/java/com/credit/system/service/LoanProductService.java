package com.credit.system.service;

import com.credit.system.domain.LoanProduct;
import com.credit.system.domain.enums.ProductStatus;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface LoanProductService {

    LoanProduct createProduct(LoanProduct product, String operator);

    LoanProduct updateProduct(Long id, LoanProduct productDetails, String operator);

    Optional<LoanProduct> getProductById(Long id);

    Optional<LoanProduct> getProductByCode(String productCode);

    Page<LoanProduct> getProductList(String name, String code, ProductStatus status,
                                     int page, int size);

    List<LoanProduct> getEligibleProductsForCustomer(Long customerId);

    void publishProduct(Long id, String operator);

    void unpublishProduct(Long id, String reason, String operator);

    void deleteProduct(Long id, String operator);
}
