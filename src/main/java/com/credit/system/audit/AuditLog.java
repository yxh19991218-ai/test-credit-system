package com.credit.system.audit;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * 审计日志实体 —— 记录所有关键业务操作的可追溯信息。
 * <p>
 * 通过 {@link AuditAspect} 自动记录，无需在各 Service 中手动调用。
 * </p>
 */
@Entity
@Table(name = "audit_log", indexes = {
    @Index(name = "idx_audit_entity", columnList = "entityType,entityId"),
    @Index(name = "idx_audit_operator", columnList = "operator"),
    @Index(name = "idx_audit_operation", columnList = "operation"),
    @Index(name = "idx_audit_created_at", columnList = "createdAt")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 操作类型，如 CREATE_CUSTOMER、REVIEW_APPLICATION */
    @Column(nullable = false, length = 50)
    private String operation;

    /** 操作人（从 SecurityContext 自动获取） */
    @Column(nullable = false, length = 100)
    private String operator;

    /** 操作描述 */
    @Column(length = 500)
    private String description;

    /** 实体类型，如 Customer、LoanApplication */
    @Column(length = 50)
    private String entityType;

    /** 实体 ID */
    private Long entityId;

    /** 请求 IP */
    @Column(length = 50)
    private String ipAddress;

    /** 操作是否成功 */
    @Column(nullable = false)
    private boolean success = true;

    /** 失败时的错误信息 */
    @Column(length = 1000)
    private String errorMessage;

    /** 操作时间 */
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public AuditLog() {}

    public AuditLog(String operation, String operator, String description,
                    String entityType, Long entityId, String ipAddress) {
        this.operation = operation;
        this.operator = operator;
        this.description = description;
        this.entityType = entityType;
        this.entityId = entityId;
        this.ipAddress = ipAddress;
    }

    // --- getters / setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
