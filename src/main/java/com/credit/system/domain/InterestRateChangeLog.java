package com.credit.system.domain;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import java.math.BigDecimal;
import com.credit.system.domain.enums.RateChangeType;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "interest_rate_change_logs")
@EntityListeners(AuditingEntityListener.class)
public class InterestRateChangeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contract_id", nullable = false)
    private Long contractId;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "old_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal oldRate;

    @Column(name = "new_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal newRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 20)
    private RateChangeType changeType;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "operator", nullable = false, length = 50)
    private String operator;

    @Column(name = "new_schedule_id")
    private Long newScheduleId;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;
}
