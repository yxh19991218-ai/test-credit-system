package com.credit.system.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.credit.system.domain.Customer;
import com.credit.system.domain.enums.CustomerStatus;
import com.credit.system.domain.enums.RiskLevel;

public interface CustomerRepository extends JpaRepository<Customer, Long>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<Customer> {

    /**
     * 根据身份证号查询客户
     */
    Optional<Customer> findByIdCard(String idCard);

    /**
     * 根据手机号查询客户
     */
    Optional<Customer> findByPhone(String phone);

    /**
     * 检查身份证号是否存在
     */
    boolean existsByIdCard(String idCard);

    /**
     * 检查手机号是否存在
     */
    boolean existsByPhone(String phone);

    /**
     * 根据状态查询客户
     */
    List<Customer> findByStatus(CustomerStatus status);

    /**
     * 分页查询客户
     */
    Page<Customer> findAll(Pageable pageable);

    /**
     * 根据征信分数范围查询客户
     */
    List<Customer> findByCreditScoreBetween(Integer minScore, Integer maxScore);

    /**
     * 根据创建时间范围查询客户
     */
    List<Customer> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 检查客户是否在黑名单中（身份证号）
     */
    @Query("SELECT COUNT(c) > 0 FROM Customer c WHERE c.idCard = :idCard AND c.status = 'BLACKLIST'")
    boolean existsInBlacklistByIdCard(@Param("idCard") String idCard);

    /**
     * 检查客户是否在黑名单中（手机号）
     */
    @Query("SELECT COUNT(c) > 0 FROM Customer c WHERE c.phone = :phone AND c.status = 'BLACKLIST'")
    boolean existsInBlacklistByPhone(@Param("phone") String phone);

    /**
     * 检查客户是否在制裁名单中
     */
    @Query("SELECT COUNT(c) > 0 FROM Customer c WHERE c.idCard = :idCard AND c.status = 'FROZEN' AND c.statusReason LIKE '%制裁%'")
    boolean existsInSanctionsList(@Param("idCard") String idCard);

    /**
     * 更新客户状态
     */
    @Modifying
    @Query("UPDATE Customer c SET c.status = :status, c.statusReason = :reason, c.updatedAt = :updateTime WHERE c.id = :id")
    void updateCustomerStatus(@Param("id") Long id, @Param("status") CustomerStatus status,
                             @Param("reason") String reason, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 根据客户ID查询客户详细信息（包含关联数据）
     */
    // 请确保 CustomerDocument 和 Customer 有正确的 JPA 关联后再启用
    // @Query("SELECT c FROM Customer c LEFT JOIN FETCH c.documents WHERE c.id = :id")
    // Optional<Customer> findByIdWithDocuments(@Param("id") Long id);

    /**
     * 根据风险等级查询客户
     */
    List<Customer> findByRiskLevel(RiskLevel riskLevel);
}