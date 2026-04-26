package com.credit.system.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.credit.system.domain.LoanContract;
import com.credit.system.domain.RepaymentPeriod;
import com.credit.system.domain.RepaymentPeriodStatus;
import com.credit.system.domain.RepaymentSchedule;
import com.credit.system.domain.enums.RepaymentMethod;
import com.credit.system.domain.enums.ScheduleStatus;
import com.credit.system.exception.BusinessException;
import com.credit.system.exception.ResourceNotFoundException;
import com.credit.system.repository.LoanContractRepository;
import com.credit.system.repository.RepaymentPeriodRepository;
import com.credit.system.repository.RepaymentScheduleRepository;
import com.credit.system.service.RepaymentScheduleService;

@Service
@Transactional
public class RepaymentScheduleServiceImpl implements RepaymentScheduleService {

    @Autowired
    private RepaymentScheduleRepository scheduleRepository;

    @Autowired
    private RepaymentPeriodRepository periodRepository;

    @Autowired
    private LoanContractRepository contractRepository;

    @Override
    public RepaymentSchedule generateSchedule(Long contractId) {
        LoanContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("合同不存在，ID: " + contractId));

        return generateSchedule(contractId,
                contract.getTotalAmount(),
                contract.getInterestRate(),
                contract.getTerm(),
                contract.getRepaymentMethod().name(),
                contract.getStartDate());
    }

    @Override
    public RepaymentSchedule generateSchedule(Long contractId,
                                               BigDecimal principal,
                                               BigDecimal annualRate,
                                               int term,
                                               String repaymentMethodStr,
                                               LocalDate startDate) {
        RepaymentMethod method;
        try {
            method = RepaymentMethod.valueOf(repaymentMethodStr);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("不支持的还款方式: " + repaymentMethodStr);
        }

        // 创建还款计划主表
        RepaymentSchedule schedule = new RepaymentSchedule();
        schedule.setContractId(contractId);

        // 从合同获取 contractNo
        String contractNo = contractRepository.findById(contractId)
                .map(c -> c.getContractNo()).orElse(null);
        schedule.setContractNo(contractNo);

        schedule.setPrincipal(principal);
        schedule.setAnnualRate(annualRate);
        schedule.setTerm(term);
        schedule.setTotalPeriods(term);
        schedule.setRepaymentMethod(method);
        schedule.setStartDate(startDate);
        schedule.setFirstPaymentDate(startDate.plusMonths(1));
        schedule.setLastPaymentDate(startDate.plusMonths(term));
        schedule.setStatus(ScheduleStatus.ACTIVE);

        // 计算每期还款
        List<RepaymentPeriod> periods = calculatePeriods(principal, annualRate, term, method, startDate, schedule);

        BigDecimal totalPayment = periods.stream()
                .map(RepaymentPeriod::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalInterest = periods.stream()
                .map(RepaymentPeriod::getInterest)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        schedule.setTotalPayment(totalPayment);
        schedule.setTotalInterest(totalInterest);
        schedule.setIrr(annualRate.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP));
        schedule.setApr(annualRate);

        schedule.setPeriods(new ArrayList<>(periods));
        schedule = scheduleRepository.save(schedule);

        return schedule;
    }

    private List<RepaymentPeriod> calculatePeriods(BigDecimal principal,
                                                   BigDecimal annualRate,
                                                   int term,
                                                   RepaymentMethod method,
                                                   LocalDate startDate,
                                                   RepaymentSchedule schedule) {
        List<RepaymentPeriod> periods = new ArrayList<>();
        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
        BigDecimal remaining = principal;

        for (int i = 1; i <= term; i++) {
            RepaymentPeriod period = new RepaymentPeriod();
            period.setPeriodNo(i);
            period.setDueDate(startDate.plusMonths(i));
            period.setStatus(RepaymentPeriodStatus.PENDING);

            switch (method) {
                case EQUAL_INSTALLMENT:
                    calculateEqualInstallment(period, principal, monthlyRate, term, i, remaining);
                    break;
                case EQUAL_PRINCIPAL:
                    calculateEqualPrincipal(period, principal, monthlyRate, term, i, remaining);
                    break;
                case INTEREST_ONLY:
                    calculateInterestOnly(period, principal, monthlyRate, term, i, remaining);
                    break;
                case BALLOON:
                    calculateBalloon(period, principal, monthlyRate, term, i, remaining);
                    break;
                case DUE_ONE_TIME:
                    calculateDueOneTime(period, principal, annualRate, term, i);
                    break;
            }

            // 扣减剩余本金
            if (method != RepaymentMethod.INTEREST_ONLY && method != RepaymentMethod.DUE_ONE_TIME) {
                remaining = remaining.subtract(period.getPrincipal());
            } else if (method == RepaymentMethod.DUE_ONE_TIME && i == term) {
                remaining = BigDecimal.ZERO;
            }
            if (remaining.compareTo(BigDecimal.ZERO) < 0) remaining = BigDecimal.ZERO;
            period.setRemainingPrincipal(remaining);

            periods.add(period);
        }

        return periods;
    }

    private void calculateEqualInstallment(RepaymentPeriod period, BigDecimal principal,
                                           BigDecimal monthlyRate, int term,
                                           int periodNo, BigDecimal remaining) {
        // 等额本息公式：每月还款额 = P * r * (1+r)^n / ((1+r)^n - 1)
        double p = principal.doubleValue();
        double r = monthlyRate.doubleValue();
        double n = term;

        double monthlyPayment = p * r * Math.pow(1 + r, n) / (Math.pow(1 + r, n) - 1);
        BigDecimal monthly = BigDecimal.valueOf(monthlyPayment).setScale(2, RoundingMode.HALF_UP);

        // 最后1期修正余差
        if (periodNo == term) {
            monthly = remaining.add(remaining.multiply(monthlyRate))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal interest = remaining.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal principalPart = monthly.subtract(interest);
        if (principalPart.compareTo(BigDecimal.ZERO) < 0) principalPart = BigDecimal.ZERO;

        period.setTotalAmount(monthly);
        period.setPrincipal(principalPart);
        period.setInterest(interest);
    }

    private void calculateEqualPrincipal(RepaymentPeriod period, BigDecimal principal,
                                         BigDecimal monthlyRate, int term,
                                         int periodNo, BigDecimal remaining) {
        // 等额本金：每期本金 = P/n
        BigDecimal principalPerPeriod = principal.divide(BigDecimal.valueOf(term), 2, RoundingMode.HALF_UP);

        // 最后1期修正
        BigDecimal actualPrincipal = principalPerPeriod;
        if (periodNo == term) {
            // 前 n-1 期已经支付了 (n-1) * (P/n)
            BigDecimal paid = principalPerPeriod.multiply(BigDecimal.valueOf(term - 1));
            actualPrincipal = principal.subtract(paid);
            if (actualPrincipal.compareTo(BigDecimal.ZERO) < 0) actualPrincipal = BigDecimal.ZERO;
        }

        BigDecimal interest = remaining.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = actualPrincipal.add(interest);

        period.setTotalAmount(total);
        period.setPrincipal(actualPrincipal);
        period.setInterest(interest);
    }

    private void calculateInterestOnly(RepaymentPeriod period, BigDecimal principal,
                                       BigDecimal monthlyRate, int term,
                                       int periodNo, BigDecimal remaining) {
        // 先息后本：前 n-1 期只还利息，最后1期还本+息
        BigDecimal interest = principal.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);

        if (periodNo < term) {
            period.setTotalAmount(interest);
            period.setPrincipal(BigDecimal.ZERO);
            period.setInterest(interest);
        } else {
            period.setTotalAmount(principal.add(interest));
            period.setPrincipal(principal);
            period.setInterest(interest);
        }
    }

    private void calculateBalloon(RepaymentPeriod period, BigDecimal principal,
                                  BigDecimal monthlyRate, int term,
                                  int periodNo, BigDecimal remaining) {
        // 气球贷：按长期限计算月供，最后1期还大额本金
        BigDecimal interest = remaining.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);

        // 模拟月供基数（按 full amortization 计算）
        double p = principal.doubleValue();
        double r = monthlyRate.doubleValue();
        double n = term;
        double monthlyPayment = p * r * Math.pow(1 + r, n) / (Math.pow(1 + r, n) - 1);
        BigDecimal monthly = BigDecimal.valueOf(monthlyPayment).setScale(2, RoundingMode.HALF_UP);

        if (periodNo < term) {
            BigDecimal principalPart = monthly.subtract(interest);
            if (principalPart.compareTo(BigDecimal.ZERO) < 0) principalPart = BigDecimal.ZERO;
            period.setTotalAmount(monthly);
            period.setPrincipal(principalPart);
            period.setInterest(interest);
        } else {
            // 最后1期还剩余本金 + 利息
            period.setTotalAmount(remaining.add(interest));
            period.setPrincipal(remaining);
            period.setInterest(interest);
        }
    }

    private void calculateDueOneTime(RepaymentPeriod period, BigDecimal principal,
                                     BigDecimal annualRate, int term, int periodNo) {
        // 到期一次性还本付息
        if (periodNo < term) {
            period.setTotalAmount(BigDecimal.ZERO);
            period.setPrincipal(BigDecimal.ZERO);
            period.setInterest(BigDecimal.ZERO);
        } else {
            BigDecimal totalInterest = principal.multiply(annualRate)
                    .multiply(BigDecimal.valueOf(term))
                    .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
            period.setTotalAmount(principal.add(totalInterest));
            period.setPrincipal(principal);
            period.setInterest(totalInterest);
        }
    }

    @Override
    public Optional<RepaymentSchedule> getScheduleByContractId(Long contractId) {
        return scheduleRepository.findByContractId(contractId);
    }

    @Override
    public Optional<RepaymentSchedule> getScheduleByContractIdWithPeriods(Long contractId) {
        return scheduleRepository.findByContractIdWithPeriods(contractId);
    }

    @Override
    public List<RepaymentPeriod> getPeriodsByScheduleId(Long scheduleId) {
        return periodRepository.findByScheduleIdOrderByPeriodNoAsc(scheduleId);
    }

    @Override
    public RepaymentPeriod getCurrentPeriod(Long scheduleId) {
        List<RepaymentPeriod> periods = periodRepository.findByScheduleIdOrderByPeriodNoAsc(scheduleId);
        return periods.stream()
                .filter(p -> p.getStatus() == RepaymentPeriodStatus.PENDING)
                .findFirst()
                .orElse(null);
    }

    @Override
    public void makePayment(Long scheduleId, Long periodId, BigDecimal amount) {
        RepaymentPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new ResourceNotFoundException("还款期次不存在，ID: " + periodId));

        if (period.getStatus() == RepaymentPeriodStatus.PAID) {
            throw new BusinessException("该期次已还款");
        }

        period.setPaidDate(LocalDate.now());
        period.setPaidAmount(amount);

        if (amount.compareTo(period.getTotalAmount()) >= 0) {
            period.setStatus(RepaymentPeriodStatus.PAID);
        } else if (amount.compareTo(BigDecimal.ZERO) > 0) {
            period.setStatus(RepaymentPeriodStatus.PARTIAL);
        }

        periodRepository.save(period);

        // 检查是否全部结清
        if (isScheduleCompleted(scheduleId)) {
            RepaymentSchedule schedule = scheduleRepository.findById(scheduleId).orElse(null);
            if (schedule != null) {
                schedule.setStatus(ScheduleStatus.COMPLETED);
                scheduleRepository.save(schedule);
            }
        }
    }

    @Override
    public void markOverduePeriods() {
        List<RepaymentPeriod> overduePeriods = periodRepository.findOverduePeriods(LocalDate.now());
        for (RepaymentPeriod period : overduePeriods) {
            long days = LocalDate.now().toEpochDay() - period.getDueDate().toEpochDay();
            period.setOverdueDays((int) days);
            period.setOverdueFine(calculateOverdueFine(period.getTotalAmount(), (int) days));
            period.setStatus(RepaymentPeriodStatus.OVERDUE);
            periodRepository.save(period);
        }
    }

    @Override
    public void modifySchedule(Long scheduleId, int newTerm, String reason, String operator) {
        RepaymentSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("还款计划不存在，ID: " + scheduleId));

        if (schedule.getStatus() != ScheduleStatus.ACTIVE) {
            throw new BusinessException("仅活跃的还款计划可以修改");
        }

        // 备份原计划为历史版本
        schedule.setModificationType("TERM_ADJUSTMENT");
        schedule.setModificationReason(reason);
        schedule.setModifiedBy(operator);
        schedule.setModifiedAt(java.time.LocalDateTime.now());

        // 重新生成
        RepaymentSchedule newSchedule = generateSchedule(
                schedule.getContractId(),
                schedule.getPrincipal(),
                schedule.getAnnualRate(),
                newTerm,
                schedule.getRepaymentMethod().name(),
                schedule.getStartDate());
        newSchedule.setPreviousVersionId(schedule.getId());
        scheduleRepository.save(newSchedule);
    }

    private boolean isScheduleCompleted(Long scheduleId) {
        List<RepaymentPeriod> periods = periodRepository.findByScheduleIdOrderByPeriodNoAsc(scheduleId);
        return periods.stream().allMatch(p -> p.getStatus() == RepaymentPeriodStatus.PAID);
    }

    private BigDecimal calculateOverdueFine(BigDecimal amount, int overdueDays) {
        // 每天罚息率 0.05%
        BigDecimal dailyRate = new BigDecimal("0.0005");
        return amount.multiply(dailyRate)
                .multiply(BigDecimal.valueOf(overdueDays))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
