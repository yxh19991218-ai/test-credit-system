package com.credit.system.repository;

import com.credit.system.domain.LoanProduct;
import com.credit.system.domain.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LoanProductRepository extends JpaRepository<LoanProduct, Long> {

    /**
     * 根据产品代码查询产品
     */
    Optional<LoanProduct> findByProductCode(String productCode);

    /**
     * 检查产品代码是否存在
     */
    boolean existsByProductCode(String productCode);

    /**
     * 根据状态查询产品
     */
    List<LoanProduct> findByStatus(ProductStatus status);

    /**
     * 根据客户ID查询适用产品（考虑客户资格和产品互斥）
     */
    @Query("SELECT p FROM LoanProduct p WHERE p.status = 'ACTIVE' " +
           "AND (p.minAge IS NULL OR p.minAge <= (SELECT 18 FROM Customer c WHERE c.id = :customerId)) " +
           "AND (p.maxAge IS NULL OR p.maxAge >= (SELECT 18 FROM Customer c WHERE c.id = :customerId)) " +
           "AND (p.minCreditScore IS NULL OR p.minCreditScore <= (SELECT c.creditScore FROM Customer c WHERE c.id = :customerId)) " +
           "AND (p.minMonthlyIncome IS NULL OR p.minMonthlyIncome <= (SELECT c.monthlyIncome FROM Customer c WHERE c.id = :customerId))")
    List<LoanProduct> findEligibleProductsForCustomer(@Param("customerId") Long customerId);

    /**
     * 根据产品名称模糊查询
     */
    List<LoanProduct> findByProductNameContaining(String productName);

    Page<LoanProduct> findByProductNameContaining(String productName, Pageable pageable);

    /**
     * 根据有效期查询活跃产品
     */
    List<LoanProduct> findByStatusAndValidFromLessThanEqualAndValidToGreaterThanEqual(
        ProductStatus status, LocalDate date1, LocalDate date2);

    /**
     * 更新产品状态
     */
    @Modifying
    @Query("UPDATE LoanProduct p SET p.status = :status, p.updatedBy = :operator, p.updatedAt = :updateTime WHERE p.id = :id")
    void updateProductStatus(@Param("id") Long id, @Param("status") ProductStatus status,
                             @Param("operator") String operator, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 检查客户是否有活跃贷款使用该产品
     */
    @Query("SELECT COUNT(l) > 0 FROM LoanContract l WHERE l.productId = :productId AND l.status = 'ACTIVE'")
    boolean hasActiveLoans(@Param("productId") Long productId);
}