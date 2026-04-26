package com.credit.system.dto;

import com.credit.system.domain.RepaymentPeriod;
import com.credit.system.domain.RepaymentSchedule;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class RepaymentScheduleResponse {
    private Long id;
    private Long contractId;
    private String contractNo;
    private BigDecimal principal;
    private BigDecimal annualRate;
    private Integer term;
    private Integer totalPeriods;
    private String repaymentMethod;
    private LocalDate startDate;
    private LocalDate firstPaymentDate;
    private LocalDate lastPaymentDate;
    private BigDecimal totalPayment;
    private BigDecimal totalInterest;
    private String status;
    private List<PeriodResponse> periods;

    @Data
    public static class PeriodResponse {
        private Integer periodNo;
        private LocalDate dueDate;
        private BigDecimal totalAmount;
        private BigDecimal principal;
        private BigDecimal interest;
        private BigDecimal remainingPrincipal;
        private String status;
        private LocalDate paidDate;
        private BigDecimal paidAmount;
        private Integer overdueDays;
        private BigDecimal overdueFine;

        public static PeriodResponse from(RepaymentPeriod p) {
            PeriodResponse r = new PeriodResponse();
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

    public static RepaymentScheduleResponse from(RepaymentSchedule s) {
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
                    .map(PeriodResponse::from)
                    .collect(Collectors.toList()));
        }
        return r;
    }
}
