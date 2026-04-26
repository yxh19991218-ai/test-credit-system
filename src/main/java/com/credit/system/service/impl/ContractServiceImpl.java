package com.credit.system.service.impl;

import com.credit.system.domain.LoanContract;
import com.credit.system.domain.LoanApplication;
import com.credit.system.domain.Customer;
import com.credit.system.domain.enums.ContractStatus;
import com.credit.system.exception.BusinessException;
import com.credit.system.exception.ResourceNotFoundException;
import com.credit.system.repository.LoanContractRepository;
import com.credit.system.repository.LoanApplicationRepository;
import com.credit.system.repository.CustomerRepository;
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
    private CustomerRepository customerRepository;

    @Override
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
    public void terminateContract(Long id, String reason, String operator) {
        LoanContract contract = contractRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("合同不存在，ID: " + id));

        if (contract.getStatus() == ContractStatus.SETTLED) {
            throw new BusinessException("已结清合同不能终止");
        }

        contractRepository.terminateContract(id, ContractStatus.BAD_DEBT, reason);
    }

    @Override
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
