package com.credit.system.domain;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import com.credit.system.domain.enums.ApplicationStatus;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "loan_applications")
@EntityListeners(AuditingEntityListener.class)
public class LoanApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "客户ID不能为空")
    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @NotNull(message = "产品ID不能为空")
    @Column(name = "product_id", nullable = false)
    private Long productId;

    @NotNull(message = "申请金额不能为空")
    @DecimalMin(value = "1000", message = "申请金额不能低于1000元")
    @Column(name = "apply_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal applyAmount;

    @NotNull(message = "申请期限不能为空")
    @Min(value = 3, message = "申请期限不能少于3个月")
    @Max(value = 36, message = "申请期限不能超过36个月")
    @Column(name = "apply_term", nullable = false)
    private Integer applyTerm;

    @Size(max = 500, message = "贷款用途描述不能超过500个字符")
    @Column(length = 500)
    private String purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status = ApplicationStatus.DRAFT;

    @Column(name = "credit_score")
    private Integer creditScore;

    @Column(name = "approved_amount", precision = 12, scale = 2)
    private BigDecimal approvedAmount;

    @Column(name = "approved_term")
    private Integer approvedTerm;

    @Column(name = "interest_rate", precision = 5, scale = 4)
    private BigDecimal interestRate;

    @Column(name = "monthly_payment", precision = 12, scale = 2)
    private BigDecimal monthlyPayment;

    @Column(name = "application_date", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime applicationDate;

    @Column(name = "submit_date")
    private LocalDateTime submitDate;

    @Column(name = "review_date")
    private LocalDateTime reviewDate;

    @Column(name = "reviewer", length = 50)
    private String reviewer;

    @Column(name = "review_comments", length = 2000)
    private String reviewComments;

    @Column(name = "latest_rate_change_id")
    private Long latestRateChangeId;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @LastModifiedDate
    private LocalDateTime updatedAt;
}