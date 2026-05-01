package com.credit.system.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 审计日志 Repository。
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
