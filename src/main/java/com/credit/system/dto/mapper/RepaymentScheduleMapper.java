package com.credit.system.dto.mapper;

import com.credit.system.domain.RepaymentPeriod;
import com.credit.system.domain.RepaymentSchedule;
import com.credit.system.dto.RepaymentScheduleResponse;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * 还款计划实体 ↔ DTO 转换映射器。
 */
@Component
public class RepaymentScheduleMapper implements EntityMapper<RepaymentSchedule, RepaymentScheduleResponse> {

    @Override
    public RepaymentScheduleResponse toDto(RepaymentSchedule s) {
        if (s == null) return null;
        RepaymentScheduleResponse r = new RepaymentScheduleResponse();
        r.setId(s.getId());
        r.setContractId(s.getContractId());
        r.setContractNo(s.getContractNo());
        r.setPrincipal(s.getPrincipal());
        r.setAnnualRate(s.getAnnualRate());
        r.setTerm(s.getTerm());
        r.setTotalPeriods(s.getTotalPeriods());
        r.setRepaymentMethod(s.getRepaymentMethod() != null ? s.getRepaymentMethod().name() : null);
        r.setStartDate(s.getStartDate());
        r.setFirstPaymentDate(s.getFirstPaymentDate());
        r.setLastPaymentDate(s.getLastPaymentDate());
        r.setTotalPayment(s.getTotalPayment());
        r.setTotalInterest(s.getTotalInterest());
        r.setStatus(s.getStatus() != null ? s.getStatus().name() : null);
        if (s.getPeriods() != null) {
            r.setPeriods(s.getPeriods().stream()
                    .map(this::toPeriodDto)
                    .collect(Collectors.toList()));
        }
        return r;
    }

    @Override
    public RepaymentSchedule toEntity(RepaymentScheduleResponse dto) {
        throw new UnsupportedOperationException("RepaymentScheduleResponse 不支持转换为实体");
    }

    /** 转换还款期次实体为 DTO。 */
    public RepaymentScheduleResponse.PeriodResponse toPeriodDto(RepaymentPeriod p) {
        if (p == null) return null;
        RepaymentScheduleResponse.PeriodResponse r = new RepaymentScheduleResponse.PeriodResponse();
        r.setPeriodNo(p.getPeriodNo());
        r.setDueDate(p.getDueDate());
        r.setTotalAmount(p.getTotalAmount());
        r.setPrincipal(p.getPrincipal());
        r.setInterest(p.getInterest());
        r.setRemainingPrincipal(p.getRemainingPrincipal());
        r.setStatus(p.getStatus() != null ? p.getStatus().name() : null);
        r.setPaidDate(p.getPaidDate());
        r.setPaidAmount(p.getPaidAmount());
        r.setOverdueDays(p.getOverdueDays());
        r.setOverdueFine(p.getOverdueFine());
        return r;
    }
}
