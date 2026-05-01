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

import com.credit.system.audit.AuditLoggable;
import com.credit.system.domain.Customer;
import com.credit.system.domain.LoanApplication;
import com.credit.system.domain.LoanProduct;
import com.credit.system.domain.enums.ApplicationStatus;
import com.credit.system.domain.enums.ProductStatus;
import com.credit.system.event.ApplicationApprovedEvent;
import com.credit.system.exception.BusinessException;
import com.credit.system.exception.ResourceNotFoundException;
import com.credit.system.repository.CustomerRepository;
import com.credit.system.repository.LoanApplicationRepository;
import com.credit.system.repository.LoanProductRepository;
import com.credit.system.service.LoanApplicationService;
import com.credit.system.service.calculator.RepaymentCalculator;
import com.credit.system.service.calculator.RepaymentCalculatorRegistry;
import com.credit.system.event.EventBus;

import jakarta.persistence.criteria.Predicate;

@Service
@Transactional
public class LoanApplicationServiceImpl implements LoanApplicationService {

    private static final Logger log = LoggerFactory.getLogger(LoanApplicationServiceImpl.class);

    private final LoanApplicationRepository applicationRepository;
    private final LoanProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final EventBus eventBus;
    private final RepaymentCalculatorRegistry calculatorRegistry;

    @Autowired
    public LoanApplicationServiceImpl(
            LoanApplicationRepository applicationRepository,
            LoanProductRepository productRepository,
            CustomerRepository customerRepository,
            EventBus eventBus,
            RepaymentCalculatorRegistry calculatorRegistry) {
        this.applicationRepository = applicationRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.eventBus = eventBus;
        this.calculatorRegistry = calculatorRegistry;
    }

    @Override
    @AuditLoggable(operation = "CREATE_APPLICATION", entityType = "LoanApplication",
            description = "创建贷款申请，客户 {0.customerId}")
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
    @AuditLoggable(operation = "SUBMIT_APPLICATION", entityType = "LoanApplication",
            description = "提交贷款申请 {0}")
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
    @AuditLoggable(operation = "REVIEW_APPLICATION", entityType = "LoanApplication",
            description = "审核贷款申请 {0} → {1}")
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

        if (decision == ApplicationStatus.APPROVED) {
            eventBus.publish(new ApplicationApprovedEvent(
                    app.getId(),
                    app.getCustomerId(),
                    app.getProductId(),
                    approvedAmount,
                    approvedTerm,
                    interestRate,
                    reviewer,
                    app.getReviewDate(),
                    comments));
        }
    }

    @Override
    @AuditLoggable(operation = "CANCEL_APPLICATION", entityType = "LoanApplication",
            description = "取消贷款申请 {0}")
    public void cancelApplication(Long id, String reason) {
        LoanApplication app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("申请不存在，ID: " + id));

        if (app.getStatus() == ApplicationStatus.COMPLETED) {
            throw new BusinessException("已完成申请不能取消");
        }

        app.setStatus(ApplicationStatus.CANCELLED);
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
        // 使用等额本息计算器计算月供
        RepaymentCalculator calculator = calculatorRegistry.getCalculator(
                com.credit.system.domain.enums.RepaymentMethod.EQUAL_INSTALLMENT);
        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(12), 10, java.math.RoundingMode.HALF_UP);
        BigDecimal remaining = principal;

        // 模拟第 1 期还款，获取月供金额
        com.credit.system.domain.RepaymentPeriod period = new com.credit.system.domain.RepaymentPeriod();
        calculator.calculate(period, principal, monthlyRate, term, 1, remaining);
        return period.getTotalAmount();
    }
}
