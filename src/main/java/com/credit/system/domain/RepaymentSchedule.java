package com.credit.system.domain;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import com.credit.system.domain.enums.RepaymentMethod;
import com.credit.system.domain.enums.ScheduleStatus;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "repayment_schedules")
@EntityListeners(AuditingEntityListener.class)
public class RepaymentSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contract_id", nullable = false)
    private Long contractId;

    @Column(name = "contract_no", nullable = false, length = 50)
    private String contractNo;

    @Column(name = "principal", nullable = false, precision = 12, scale = 2)
    private BigDecimal principal;

    @Column(name = "annual_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal annualRate;

    @Column(name = "term", nullable = false)
    private Integer term;

    @Column(name = "total_periods", nullable = false)
    private Integer totalPeriods;

    @Enumerated(EnumType.STRING)
    @Column(name = "repayment_method", nullable = false, length = 20)
    private RepaymentMethod repaymentMethod;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "first_payment_date")
    private LocalDate firstPaymentDate;

    @Column(name = "last_payment_date")
    private LocalDate lastPaymentDate;

    @Column(name = "total_payment", precision = 12, scale = 2)
    private BigDecimal totalPayment;

    @Column(name = "total_interest", precision = 12, scale = 2)
    private BigDecimal totalInterest;

    @Column(name = "irr", precision = 10, scale = 8)
    private BigDecimal irr;

    @Column(name = "apr", precision = 10, scale = 8)
    private BigDecimal apr;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ScheduleStatus status = ScheduleStatus.ACTIVE;

    @Column(name = "lock_period")
    private Integer lockPeriod;

    @Column(name = "prepayment_penalty_rate", precision = 5, scale = 4)
    private BigDecimal prepaymentPenaltyRate;

    @Column(name = "previous_version_id")
    private Long previousVersionId;

    @Column(name = "modification_type", length = 20)
    private String modificationType;

    @Column(name = "modification_reason", length = 500)
    private String modificationReason;

    @Column(name = "modified_by", length = 50)
    private String modifiedBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RepaymentPeriod> periods = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (periods != null) {
            periods.forEach(p -> p.setSchedule(this));
        }
    }
}