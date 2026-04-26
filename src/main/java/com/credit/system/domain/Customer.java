package com.credit.system.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.credit.system.domain.enums.CustomerStatus;
import com.credit.system.domain.enums.RiskLevel;

import lombok.Data;

@Data
@Entity
@Table(name = "customers")
@EntityListeners(AuditingEntityListener.class)
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 50, message = "姓名长度不能超过50个字符")
    @Column(nullable = false, length = 50)
    private String name;

    @NotBlank(message = "身份证号不能为空")
    @Pattern(regexp = "(^[1-9]\\d{5}(18|19|([23]\\d))\\d{2}((0[1-9])|(10|11|12))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$)",
            message = "身份证号格式不正确")
    @Column(nullable = false, unique = true, length = 18)
    private String idCard;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Column(nullable = false, length = 20)
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    @Column(length = 100)
    private String email;

    // 职业信息
    @Size(max = 50, message = "职业长度不能超过50个字符")
    @Column(length = 50)
    private String occupation;

    @Column(name = "monthly_income", precision = 12, scale = 2)
    private BigDecimal monthlyIncome; // 月收入

    @Size(max = 200, message = "住址长度不能超过200个字符")
    @Column(length = 200)
    private String address;

    // 征信信息
    @Column(name = "credit_score")
    private Integer creditScore; // 征信分数

    @Size(max = 50, message = "征信报告编号长度不能超过50个字符")
    @Column(name = "credit_report_no", length = 50)
    private String creditReportNo;

    // 银行信息
    @Size(max = 30, message = "银行卡号长度不能超过30个字符")
    @Column(name = "bank_card_no", length = 30)
    private String bankCardNo;

    @Size(max = 50, message = "开户行长度不能超过50个字符")
    @Column(name = "bank_name", length = 50)
    private String bankName;

    // 联系人信息（用于催收）
    @Size(max = 50, message = "紧急联系人姓名长度不能超过50个字符")
    @Column(name = "emergency_contact_name", length = 50)
    private String emergencyContactName;

    @Size(max = 20, message = "紧急联系人电话长度不能超过20个字符")
    @Column(name = "emergency_contact_phone", length = 20)
    private String emergencyContactPhone;

    // 客户状态
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CustomerStatus status = CustomerStatus.NORMAL;

    @Column(name = "status_reason", length = 500)
    private String statusReason; // 状态变更原因

    // 风险等级
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", length = 20)
    private RiskLevel riskLevel;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @LastModifiedDate
    private LocalDateTime updatedAt;

    // 合并时间
    @Column(name = "merge_time")
    private LocalDateTime mergeTime;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CustomerDocument> documents = new ArrayList<>();
}
