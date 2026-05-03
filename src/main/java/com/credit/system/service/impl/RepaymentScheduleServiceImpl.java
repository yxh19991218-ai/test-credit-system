package com.credit.system.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
import com.credit.system.service.calculator.RepaymentCalculator;
import com.credit.system.service.calculator.RepaymentCalculatorRegistry;

@Service
@Transactional
public class RepaymentScheduleServiceImpl implements RepaymentScheduleService {

    private final RepaymentScheduleRepository scheduleRepository;
    private final RepaymentPeriodRepository periodRepository;
    private final LoanContractRepository contractRepository;
    private final RepaymentCalculatorRegistry calculatorRegistry;

    @Autowired
    public RepaymentScheduleServiceImpl(
            RepaymentScheduleRepository scheduleRepository,
            RepaymentPeriodRepository periodRepository,
            LoanContractRepository contractRepository,
            RepaymentCalculatorRegistry calculatorRegistry) {
        this.scheduleRepository = scheduleRepository;
        this.periodRepository = periodRepository;
        this.contractRepository = contractRepository;
        this.calculatorRegistry = calculatorRegistry;
    }

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

        RepaymentCalculator calculator = calculatorRegistry.getCalculator(method);

        for (int i = 1; i <= term; i++) {
            RepaymentPeriod period = new RepaymentPeriod();
            period.setPeriodNo(i);
            period.setDueDate(startDate.plusMonths(i));
            period.setStatus(RepaymentPeriodStatus.PENDING);

            calculator.calculate(period, principal, monthlyRate, term, i, remaining);

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

    @Override
    public RepaymentSchedule changeInterestRate(Long contractId, BigDecimal newRate, String reason, String operator) {
        RepaymentSchedule oldSchedule = scheduleRepository.findActiveByContractId(contractId)
                .orElseThrow(() -> new BusinessException("还款计划不存在或已挂起"));

        List<RepaymentPeriod> allPeriods = periodRepository.findByScheduleIdOrderByPeriodNoAsc(oldSchedule.getId());
        List<RepaymentPeriod> paidPeriods = allPeriods.stream()
                .filter(p -> p.getStatus() == RepaymentPeriodStatus.PAID || p.getStatus() == RepaymentPeriodStatus.PARTIAL)
                .collect(Collectors.toList());

        int paidCount = paidPeriods.size();
        BigDecimal paidPrincipal = paidPeriods.stream()
                .map(RepaymentPeriod::getPrincipal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remainingPrincipal = oldSchedule.getPrincipal().subtract(paidPrincipal);
        int remainingTerms = allPeriods.size() - paidCount;

        if (remainingTerms <= 0) {
            throw new BusinessException("合同已全部还清，无需变更利率");
        }

        LoanContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("合同不存在"));

        LocalDate nextStartDate = paidCount > 0
                ? paidPeriods.get(paidCount - 1).getDueDate()
                : oldSchedule.getStartDate();

        RepaymentSchedule newSchedule = generateSchedule(
                contractId, remainingPrincipal, newRate, remainingTerms,
                contract.getRepaymentMethod().name(), nextStartDate);

        // Adjust period numbers and due dates for future periods
        List<RepaymentPeriod> generatedPeriods = new ArrayList<>(newSchedule.getPeriods());
        for (int i = 0; i < generatedPeriods.size(); i++) {
            RepaymentPeriod p = generatedPeriods.get(i);
            p.setPeriodNo(paidCount + i + 1);
            p.setDueDate(oldSchedule.getStartDate().plusMonths(paidCount + i + 1));
        }

        // Copy paid periods as new entities
        List<RepaymentPeriod> paidCopies = new ArrayList<>();
        for (RepaymentPeriod p : paidPeriods) {
            RepaymentPeriod copy = new RepaymentPeriod();
            copy.setPeriodNo(p.getPeriodNo());
            copy.setDueDate(p.getDueDate());
            copy.setTotalAmount(p.getTotalAmount());
            copy.setPrincipal(p.getPrincipal());
            copy.setInterest(p.getInterest());
            copy.setRemainingPrincipal(p.getRemainingPrincipal());
            copy.setStatus(p.getStatus());
            copy.setPaidDate(p.getPaidDate());
            copy.setPaidAmount(p.getPaidAmount());
            copy.setOverdueDays(p.getOverdueDays());
            copy.setOverdueFine(p.getOverdueFine());
            copy.setSchedule(newSchedule);
            paidCopies.add(copy);
        }

        // Prepend paid copies to the new schedule's periods
        newSchedule.getPeriods().addAll(0, paidCopies);

        // Update new schedule metadata
        newSchedule.setTerm(oldSchedule.getTerm());
        newSchedule.setTotalPeriods(oldSchedule.getTerm());
        newSchedule.setPreviousVersionId(oldSchedule.getId());
        newSchedule.setModificationType("INTEREST_RATE_CHANGE");
        newSchedule.setModificationReason(reason);
        newSchedule.setModifiedBy(operator);
        newSchedule.setModifiedAt(LocalDateTime.now());

        // Mark old schedule as SUSPENDED
        oldSchedule.setStatus(ScheduleStatus.SUSPENDED);

        scheduleRepository.save(oldSchedule);
        scheduleRepository.save(newSchedule);

        return newSchedule;
    }
}
