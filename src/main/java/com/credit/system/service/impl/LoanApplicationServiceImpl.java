package com.credit.system.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.credit.system.domain.Customer;
import com.credit.system.domain.LoanApplication;
import com.credit.system.domain.LoanContract;
import com.credit.system.domain.LoanProduct;
import com.credit.system.domain.enums.ApplicationStatus;
import com.credit.system.domain.enums.ProductStatus;
import com.credit.system.exception.BusinessException;
import com.credit.system.exception.ResourceNotFoundException;
import com.credit.system.repository.CustomerRepository;
import com.credit.system.repository.LoanApplicationRepository;
import com.credit.system.repository.LoanContractRepository;
import com.credit.system.repository.LoanProductRepository;
import com.credit.system.service.LoanApplicationService;
import com.credit.system.service.RepaymentScheduleService;

import jakarta.persistence.criteria.Predicate;

@Service
@Transactional
public class LoanApplicationServiceImpl implements LoanApplicationService {

    private static final Logger log = LoggerFactory.getLogger(LoanApplicationServiceImpl.class);

    @Autowired
    private LoanApplicationRepository applicationRepository;

    @Autowired
    private LoanProductRepository productRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private LoanContractRepository contractRepository;

    @Autowired
    private RepaymentScheduleService scheduleService;

    @Override
    public LoanApplication createApplication(LoanApplication application, String operator) {
        // 验证客户存在
        Customer customer = customerRepository.findById(application.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("客户不存在"));

        // 验证产品存在且活跃
        LoanProduct product = productRepository.findById(application.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("产品不存在"));

        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new BusinessException("产品未上架");
        }

        // 验证产品规则
        validateProductRules(customer, product, application);

        // 设置默认值
        application.setStatus(ApplicationStatus.DRAFT);
        application.setApplicationDate(LocalDateTime.now());

        return applicationRepository.save(application);
    }

    @Override
    public LoanApplication updateApplication(Long id, LoanApplication details) {
        LoanApplication app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("申请不存在，ID: " + id));

        if (app.getStatus() != ApplicationStatus.DRAFT) {
            throw new BusinessException("仅草稿状态的申请可以修改");
        }

        if (details.getApplyAmount() != null) app.setApplyAmount(details.getApplyAmount());
        if (details.getApplyTerm() != null) app.setApplyTerm(details.getApplyTerm());
        if (details.getPurpose() != null) app.setPurpose(details.getPurpose());

        return applicationRepository.save(app);
    }

    @Override
    public Optional<LoanApplication> getApplicationById(Long id) {
        return applicationRepository.findById(id);
    }

    @Override
    public Optional<LoanApplication> getApplicationByContractId(Long contractId) {
        return applicationRepository.findByContractId(contractId);
    }

    @Override
    public Page<LoanApplication> getApplicationsByCustomerId(Long customerId, int page, int size) {
        return applicationRepository.findByCustomerId(customerId, PageRequest.of(page, size));
    }

    @Override
    public Page<LoanApplication> getApplicationList(Long customerId, Long productId,
                                                     ApplicationStatus status, int page, int size) {
        Specification<LoanApplication> spec = (root, query, cb) -> {
            Predicate predicate = cb.conjunction();
            if (customerId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("customerId"), customerId));
            }
            if (productId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("productId"), productId));
            }
            if (status != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }
            return predicate;
        };
        return applicationRepository.findAll(spec, PageRequest.of(page, size));
    }

    @Override
    public void submitApplication(Long id) {
        LoanApplication app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("申请不存在，ID: " + id));

        if (app.getStatus() != ApplicationStatus.DRAFT) {
            throw new BusinessException("仅草稿状态的申请可以提交");
        }

        app.setStatus(ApplicationStatus.PENDING);
        app.setSubmitDate(LocalDateTime.now());
        applicationRepository.save(app);
    }

    @Override
    public void reviewApplication(Long id, ApplicationStatus decision, String reviewer,
                                  String comments, BigDecimal approvedAmount,
                                  Integer approvedTerm, BigDecimal interestRate) {
        LoanApplication app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("申请不存在，ID: " + id));

        if (app.getStatus() != ApplicationStatus.PENDING) {
            throw new BusinessException("仅待审批状态的申请可以审核");
        }

        app.setStatus(decision);
        app.setReviewer(reviewer);
        app.setReviewDate(LocalDateTime.now());
        app.setReviewComments(comments);

        if (decision == ApplicationStatus.APPROVED) {
            app.setApprovedAmount(approvedAmount);
            app.setApprovedTerm(approvedTerm);
            app.setInterestRate(interestRate);
            app.setMonthlyPayment(calculateMonthlyPayment(
                    approvedAmount, interestRate, approvedTerm));
        }

        applicationRepository.save(app);
    }

    @Override
    public void cancelApplication(Long id, String reason) {
        LoanApplication app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("申请不存在，ID: " + id));

        if (app.getStatus() == ApplicationStatus.COMPLETED) {
            throw new BusinessException("已完成申请不能取消");
        }

        app.setStatus(ApplicationStatus.CANCELLED);
        applicationRepository.save(app);
    }

    @Override
    public void approveToContract(Long applicationId, Long contractId) {
        LoanApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("申请不存在"));

        LoanContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("合同不存在"));

        if (app.getStatus() != ApplicationStatus.APPROVED) {
            throw new BusinessException("仅已审批的申请可以生成合同");
        }

        app.setStatus(ApplicationStatus.COMPLETED);
        app.setContractId(contract.getId());

        // 生成还款计划
        scheduleService.generateSchedule(contract.getId());
        applicationRepository.save(app);
    }

    private void validateProductRules(Customer customer, LoanProduct product, LoanApplication application) {
        // 金额范围
        if (application.getApplyAmount().compareTo(product.getMinAmount()) < 0
                || application.getApplyAmount().compareTo(product.getMaxAmount()) > 0) {
            throw new BusinessException(String.format("申请金额必须在 %.2f ~ %.2f 之间",
                    product.getMinAmount(), product.getMaxAmount()));
        }

        // 期限范围
        if (application.getApplyTerm() < product.getMinTerm()
                || application.getApplyTerm() > product.getMaxTerm()) {
            throw new BusinessException(String.format("申请期限必须在 %d ~ %d 之间",
                    product.getMinTerm(), product.getMaxTerm()));
        }

        // 征信分数
        if (product.getMinCreditScore() != null
                && (customer.getCreditScore() == null
                    || customer.getCreditScore() < product.getMinCreditScore())) {
            throw new BusinessException("征信分数不满足产品要求");
        }

        // 月收入
        if (product.getMinMonthlyIncome() != null
                && (customer.getMonthlyIncome() == null
                    || customer.getMonthlyIncome().compareTo(product.getMinMonthlyIncome()) < 0)) {
            throw new BusinessException("月收入不满足产品要求");
        }
    }

    private BigDecimal calculateMonthlyPayment(BigDecimal principal, BigDecimal annualRate, int term) {
        // 等额本息月供 = P * r * (1+r)^n / ((1+r)^n - 1)
        double p = principal.doubleValue();
        double r = annualRate.doubleValue() / 12.0;
        double n = term;

        double monthly = p * r * Math.pow(1 + r, n) / (Math.pow(1 + r, n) - 1);
        return BigDecimal.valueOf(Math.round(monthly * 100.0) / 100.0);
    }
}
