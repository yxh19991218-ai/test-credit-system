package com.credit.system.service.impl;

import com.credit.system.audit.AuditLoggable;
import com.credit.system.domain.LoanApplication;
import com.credit.system.domain.LoanContract;
import com.credit.system.domain.LoanProduct;
import com.credit.system.domain.Customer;
import com.credit.system.domain.enums.ApplicationStatus;
import com.credit.system.domain.enums.ContractStatus;
import com.credit.system.exception.BusinessException;
import com.credit.system.exception.ResourceNotFoundException;
import com.credit.system.event.ContractCreatedEvent;
import com.credit.system.event.EventBus;
import com.credit.system.repository.LoanApplicationRepository;
import com.credit.system.repository.LoanContractRepository;
import com.credit.system.repository.CustomerRepository;
import com.credit.system.repository.LoanProductRepository;
import com.credit.system.service.ContractService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ContractServiceImpl implements ContractService {

    private static final Logger log = LoggerFactory.getLogger(ContractServiceImpl.class);

    @Autowired
    private LoanContractRepository contractRepository;

    @Autowired
    private LoanApplicationRepository applicationRepository;

    @Autowired
    private LoanProductRepository productRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private EventBus eventBus;

    @Override
    @AuditLoggable(operation = "CREATE_CONTRACT", entityType = "LoanContract",
            description = "创建贷款合同，客户 {0.customerId}")
    public LoanContract createContract(LoanContract contract, String operator) {
        log.info("创建贷款合同，客户ID: {}, 申请ID: {}, 金额: {}", 
                 contract.getCustomerId(), contract.getApplicationId(), contract.getTotalAmount());
        
        // 验证客户
        Customer customer = customerRepository.findById(contract.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("客户不存在"));

        // 验证申请并关联
        if (contract.getApplicationId() != null) {
            LoanApplication app = applicationRepository.findById(contract.getApplicationId())
                    .orElseThrow(() -> new ResourceNotFoundException("申请不存在"));
            
            // 验证申请状态
            if (!"APPROVED".equals(app.getStatus().name())) {
                throw new BusinessException("申请状态不是已通过，无法创建合同");
            }
            
            // 验证客户匹配
            if (!app.getCustomerId().equals(contract.getCustomerId())) {
                throw new BusinessException("申请与客户不匹配");
            }
            
            log.debug("申请验证通过，申请ID: {}, 状态: {}", app.getId(), app.getStatus());
        }

        // 生成合同号
        contract.setContractNo(generateContractNo());

        // 初始状态
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setRemainingPrincipal(contract.getTotalAmount());
        contract.setPaidPeriods(0);
        contract.setExtendedMonths(0);

        LoanContract saved = contractRepository.save(contract);
        log.info("贷款合同创建成功，合同号: {}", saved.getContractNo());
        
        return saved;
    }

    @Override
    public LoanContract createContractFromApplication(Long applicationId, String operator) {
        LoanApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("申请不存在，ID: " + applicationId));

        if (application.getStatus() != ApplicationStatus.APPROVED) {
            throw new BusinessException("仅已审批的申请可以创建合同");
        }

        Optional<LoanContract> existingContract = contractRepository.findByApplicationId(applicationId);
        if (existingContract.isPresent()) {
            return existingContract.get();
        }

        LoanProduct product = productRepository.findById(application.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("产品不存在，ID: " + application.getProductId()));

        LoanContract contract = new LoanContract();
        contract.setApplicationId(applicationId);
        contract.setCustomerId(application.getCustomerId());
        contract.setProductId(application.getProductId());
        contract.setTotalAmount(application.getApprovedAmount());
        contract.setTerm(application.getApprovedTerm());
        contract.setInterestRate(application.getInterestRate());
        contract.setRepaymentMethod(product.getRepaymentMethod());
        contract.setStartDate(LocalDate.now());
        contract.setEndDate(contract.getStartDate().plusMonths(application.getApprovedTerm()));

        LoanContract saved = createContract(contract, operator);
        eventBus.publish(new ContractCreatedEvent(saved.getId()));
        return saved;
    }

    @Override
    public Optional<LoanContract> getContractById(Long id) {
        return contractRepository.findById(id);
    }

    @Override
    public Optional<LoanContract> getContractByNo(String contractNo) {
        return contractRepository.findByContractNo(contractNo);
    }

    @Override
    public Optional<LoanContract> getContractByApplicationId(Long applicationId) {
        return contractRepository.findByApplicationId(applicationId);
    }

    @Override
    public List<LoanContract> getContractsByCustomerId(Long customerId) {
        return contractRepository.findByCustomerId(customerId);
    }

    @Override
    public Page<LoanContract> getContractList(Long customerId, Long productId,
                                               ContractStatus status, int page, int size) {
        if (customerId != null) {
            return contractRepository.findByCustomerId(customerId, PageRequest.of(page, size));
        }
        return contractRepository.findAll(PageRequest.of(page, size));
    }

    @Override
    @AuditLoggable(operation = "SIGN_CONTRACT", entityType = "LoanContract",
            description = "签署合同 {0}")
    public void signContract(Long id, String signatory, String signatureMethod) {
        LoanContract contract = contractRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("合同不存在，ID: " + id));

        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new BusinessException("合同状态异常，无法签署");
        }

        contract.setSignDate(LocalDateTime.now());
        contract.setSignatory(signatory);
        contract.setSignatureMethod(signatureMethod);
        contractRepository.save(contract);
    }

    @Override
    @AuditLoggable(operation = "TERMINATE_CONTRACT", entityType = "LoanContract",
            description = "终止合同 {0}")
    public void terminateContract(Long id, String reason, String operator) {
        LoanContract contract = contractRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("合同不存在，ID: " + id));

        if (contract.getStatus() == ContractStatus.SETTLED) {
            throw new BusinessException("已结清合同不能终止");
        }

        contractRepository.terminateContract(id, ContractStatus.BAD_DEBT, reason);
    }

    @Override
    @AuditLoggable(operation = "EXTEND_CONTRACT", entityType = "LoanContract",
            description = "合同展期 {0} 延长 {1} 个月")
    public void extendContract(Long id, int months, String reason, String operator) {
        LoanContract contract = contractRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("合同不存在，ID: " + id));

        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new BusinessException("仅活跃合同可以展期");
        }

        if (contract.getExtendedMonths() + months > 12) {
            throw new BusinessException("累计展期不能超过12个月");
        }

        LocalDate newEndDate = contract.getEndDate().plusMonths(months);
        contractRepository.extendContract(id, months, reason, newEndDate);
    }

    @Override
    @AuditLoggable(operation = "SETTLE_CONTRACT", entityType = "LoanContract",
            description = "结清合同 {0}")
    public void settleContract(Long id) {
        LoanContract contract = contractRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("合同不存在，ID: " + id));

        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new BusinessException("仅活跃合同可以结清");
        }

        contract.setStatus(ContractStatus.SETTLED);
        contract.setRemainingPrincipal(java.math.BigDecimal.ZERO);
        contractRepository.save(contract);
    }

    @Override
    public List<LoanContract> getOverdueContracts() {
        return contractRepository.findOverdueContracts(LocalDate.now());
    }

    @Override
    public List<LoanContract> getContractsDueBetween(LocalDate from, LocalDate to) {
        return contractRepository.findContractsDueBetween(from, to);
    }

    private String generateContractNo() {
        String base = "LOAN" + LocalDate.now().toString().replace("-", "");
        String suffix;
        do {
            suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (contractRepository.existsByContractNo(base + suffix));
        return base + suffix;
    }
}
