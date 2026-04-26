package com.credit.system.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.credit.system.domain.RepaymentPeriod;
import com.credit.system.domain.RepaymentPeriodStatus;

public interface RepaymentPeriodRepository extends JpaRepository<RepaymentPeriod, Long> {

    @Query("SELECT p FROM RepaymentPeriod p WHERE p.schedule.id = :scheduleId")
    List<RepaymentPeriod> findByScheduleId(@Param("scheduleId") Long scheduleId);

    @Query("SELECT p FROM RepaymentPeriod p WHERE p.schedule.id = :scheduleId ORDER BY p.periodNo ASC")
    List<RepaymentPeriod> findByScheduleIdOrderByPeriodNoAsc(@Param("scheduleId") Long scheduleId);

    List<RepaymentPeriod> findByStatus(RepaymentPeriodStatus status);

    @Query("SELECT p FROM RepaymentPeriod p WHERE p.dueDate < :today AND p.status = 'PENDING'")
    List<RepaymentPeriod> findOverduePeriods(@Param("today") LocalDate today);

    @Query("SELECT COUNT(p) > 0 FROM RepaymentPeriod p WHERE p.schedule.id = :scheduleId AND p.status = 'OVERDUE'")
    boolean hasOverduePeriod(@Param("scheduleId") Long scheduleId);

    @Query("SELECT COALESCE(SUM(p.overdueFine), 0) FROM RepaymentPeriod p " +
           "WHERE p.schedule.id = :scheduleId AND p.status IN ('OVERDUE', 'PENDING')")
    BigDecimal sumOutstandingFine(@Param("scheduleId") Long scheduleId);

    @Modifying
    @Query("UPDATE RepaymentPeriod p SET p.status = 'OVERDUE', p.overdueDays = :days " +
           "WHERE p.id = :periodId AND p.status = 'PENDING'")
    void markOverdue(@Param("periodId") Long periodId, @Param("days") Integer days);

    // === 仪表盘统计查询 ===

    @Query("SELECT COALESCE(SUM(p.overdueFine + p.totalAmount), 0) FROM RepaymentPeriod p " +
           "WHERE p.status = 'OVERDUE'")
    BigDecimal sumOverdueAmount();

    @Query("SELECT COALESCE(SUM(p.totalAmount), 0) FROM RepaymentPeriod p " +
           "WHERE p.dueDate BETWEEN :from AND :to")
    BigDecimal sumTotalAmountByDueDateBetween(@Param("from") LocalDate from,
                                              @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(p.paidAmount), 0) FROM RepaymentPeriod p " +
           "WHERE p.paidDate BETWEEN :from AND :to")
    BigDecimal sumPaidAmountByPaidDateBetween(@Param("from") LocalDate from,
                                              @Param("to") LocalDate to);
}
