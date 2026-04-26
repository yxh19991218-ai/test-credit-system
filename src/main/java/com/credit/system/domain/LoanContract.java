package com.credit.system.domain;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import com.credit.system.domain.enums.ContractStatus;
import com.credit.system.domain.enums.RepaymentMethod;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "loan_contracts")
@EntityListeners(AuditingEntityListener.class)
public class LoanContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id")
    private Long applicationId;

    @Column(name = "contract_no", nullable = false, unique = true, length = 50)
    private String contractNo;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "remaining_principal", nullable = false, precision = 12, scale = 2)
    private BigDecimal remainingPrincipal;

    @Column(name = "term", nullable = false)
    private Integer term;

    @Column(name = "paid_periods", columnDefinition = "int default 0")
    private Integer paidPeriods;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal interestRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "repayment_method", nullable = false, length = 20)
    private RepaymentMethod repaymentMethod;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ContractStatus status = ContractStatus.ACTIVE;

    @Column(name = "sign_date")
    private LocalDateTime signDate;

    @Column(name = "signatory", length = 50)
    private String signatory;

    @Column(name = "signature_method", length = 20)
    private String signatureMethod;

    @Column(name = "termination_reason", length = 500)
    private String terminationReason;

    @Column(name = "extended_months", columnDefinition = "int default 0")
    private Integer extendedMonths;

    @Column(name = "extension_reason", length = 500)
    private String extensionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @LastModifiedDate
    private LocalDateTime updatedAt;
}