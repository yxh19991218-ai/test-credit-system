package com.credit.system.service;

import com.credit.system.domain.RepaymentSchedule;
import com.credit.system.domain.RepaymentPeriod;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface RepaymentScheduleService {

    RepaymentSchedule generateSchedule(Long contractId);

    RepaymentSchedule generateSchedule(Long contractId,
                                        BigDecimal principal,
                                        BigDecimal annualRate,
                                        int term,
                                        String repaymentMethod,
                                        java.time.LocalDate startDate);

    Optional<RepaymentSchedule> getScheduleByContractId(Long contractId);

    Optional<RepaymentSchedule> getScheduleByContractIdWithPeriods(Long contractId);

    List<RepaymentPeriod> getPeriodsByScheduleId(Long scheduleId);

    RepaymentPeriod getCurrentPeriod(Long scheduleId);

    void makePayment(Long scheduleId, Long periodId, BigDecimal amount);

    void markOverduePeriods();

    void modifySchedule(Long scheduleId, int newTerm, String reason, String operator);

    RepaymentSchedule changeInterestRate(Long contractId, BigDecimal newRate, String reason, String operator);
}
