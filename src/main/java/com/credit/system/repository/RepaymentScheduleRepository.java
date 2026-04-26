package com.credit.system.repository;

import com.credit.system.domain.RepaymentSchedule;
import com.credit.system.domain.enums.ScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RepaymentScheduleRepository extends JpaRepository<RepaymentSchedule, Long> {

    Optional<RepaymentSchedule> findByContractId(Long contractId);

    List<RepaymentSchedule> findByStatus(ScheduleStatus status);

    @Query("SELECT s FROM RepaymentSchedule s WHERE s.contractId IN :contractIds")
    List<RepaymentSchedule> findByContractIds(@Param("contractIds") List<Long> contractIds);

    @Query("SELECT s FROM RepaymentSchedule s WHERE s.status = 'ACTIVE' AND s.contractId = :contractId")
    Optional<RepaymentSchedule> findActiveByContractId(@Param("contractId") Long contractId);

    @Query("SELECT s FROM RepaymentSchedule s " +
           "JOIN FETCH s.periods WHERE s.contractId = :contractId")
    Optional<RepaymentSchedule> findByContractIdWithPeriods(@Param("contractId") Long contractId);
}
