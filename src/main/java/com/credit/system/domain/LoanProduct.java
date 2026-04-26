package com.credit.system.domain;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import com.credit.system.domain.enums.ProductStatus;
import com.credit.system.domain.enums.RepaymentMethod;
import com.credit.system.domain.enums.RiskLevel;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "loan_products")
@EntityListeners(AuditingEntityListener.class)
public class LoanProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "产品代码不能为空")
    @Size(max = 50, message = "产品代码长度不能超过50个字符")
    @Column(name = "product_code", nullable = false, unique = true, length = 50)
    private String productCode;

    @NotBlank(message = "产品名称不能为空")
    @Size(max = 100, message = "产品名称长度不能超过100个字符")
    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @Size(max = 500, message = "产品描述长度不能超过500个字符")
    @Column(name = "product_description", length = 500)
    private String productDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProductStatus status = ProductStatus.DRAFT;

    @NotNull(message = "利率不能为空")
    @DecimalMin(value = "0.06", message = "利率不能低于6%")
    @DecimalMax(value = "0.24", message = "利率不能高于24%")
    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal interestRate; // 年利率

    @Column(name = "min_interest_rate", precision = 5, scale = 4)
    private BigDecimal minInterestRate; // 最低利率（浮动利率）

    @Column(name = "max_interest_rate", precision = 5, scale = 4)
    private BigDecimal maxInterestRate; // 最高利率（浮动利率）

    @Column(name = "interest_rate_type", length = 20)
    private String interestRateType; // FIXED, FLOATING

    @NotNull(message = "还款方式不能为空")
    @Enumerated(EnumType.STRING)
    @Column(name = "repayment_method", nullable = false, length = 20)
    private RepaymentMethod repaymentMethod;

    @NotNull(message = "最小金额不能为空")
    @DecimalMin(value = "1000", message = "最小金额不能低于1000元")
    @Column(name = "min_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal minAmount;

    @NotNull(message = "最大金额不能为空")
    @DecimalMax(value = "1000000", message = "最大金额不能超过1000000元")
    @Column(name = "max_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal maxAmount;

    @NotNull(message = "最短期限不能为空")
    @Min(value = 3, message = "最短期限不能少于3个月")
    @Column(name = "min_term", nullable = false)
    private Integer minTerm;

    @NotNull(message = "最长期限不能为空")
    @Max(value = 36, message = "最长期限不能超过36个月")
    @Column(name = "max_term", nullable = false)
    private Integer maxTerm;

    @Column(name = "min_age")
    private Integer minAge = 18;

    @Column(name = "max_age")
    private Integer maxAge = 65;

    @Column(name = "min_credit_score")
    private Integer minCreditScore;

    @Column(name = "min_monthly_income", precision = 12, scale = 2)
    private BigDecimal minMonthlyIncome;

    @Column(name = "allowed_occupations", length = 1000)
    private String allowedOccupations; // JSON数组

    @Column(name = "allowed_regions", length = 1000)
    private String allowedRegions; // JSON数组

    @Column(name = "eligibility_rules", length = 2000)
    private String eligibilityRules; // JSON格式

    @Column(name = "excluded_product_ids", length = 1000)
    private String excludedProductIds; // JSON数组

    @Column(name = "service_fee_rate", precision = 5, scale = 4)
    private BigDecimal serviceFeeRate; // 服务费率

    @Column(name = "handling_fee", precision = 12, scale = 2)
    private BigDecimal handlingFee; // 手续费

    @Column(name = "management_fee_rate", precision = 5, scale = 4)
    private BigDecimal managementFeeRate; // 管理费率

    @Column(name = "risk_reserve_rate", precision = 5, scale = 4)
    private BigDecimal riskReserveRate; // 风险准备金率

    @Column(name = "complexity_level")
    private Integer complexityLevel; // 复杂度等级 1-5

    @Column(name = "historical_issues")
    private Integer historicalIssues; // 历史问题数量

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", length = 20)
    private RiskLevel riskLevel; // LOW, MEDIUM, HIGH

    @Column(name = "risk_description", length = 1000)
    private String riskDescription;

    @Column(name = "policy_document_url", length = 500)
    private String policyDocumentUrl; // 政策文档URL

    @Column(name = "terms_document_url", length = 500)
    private String termsDocumentUrl; // 条款文档URL

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "publish_date")
    private LocalDateTime publishDate;

    @Column(name = "published_by", length = 50)
    private String publishedBy;

    @Column(name = "unpublish_date")
    private LocalDateTime unpublishDate;

    @Column(name = "unpublish_reason", length = 500)
    private String unpublishReason;

    @Column(name = "unpublished_by", length = 50)
    private String unpublishedBy;

    @Column(name = "high_rate_approval_id", length = 100)
    private String highRateApprovalId; // 高利率审批ID

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    @Column(name = "updated_at", nullable = false)
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column(name = "version_number", length = 20)
    private String versionNumber;
}