package com.credit.system.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
import com.credit.system.audit.RequireAdmin;
import com.credit.system.domain.Customer;
import com.credit.system.domain.CustomerDocument;
import com.credit.system.domain.enums.CustomerStatus;
import com.credit.system.domain.enums.DocumentType;
import com.credit.system.domain.enums.RiskLevel;
import com.credit.system.exception.BusinessException;
import com.credit.system.exception.ResourceNotFoundException;
import com.credit.system.repository.CustomerDocumentRepository;
import com.credit.system.repository.CustomerRepository;
import com.credit.system.service.CustomerService;
import com.credit.system.service.specification.CustomerSpecification;
import com.credit.system.service.specification.SpecificationResult;
import com.credit.system.web.multipart.MultipartFile;

import jakarta.persistence.criteria.Predicate;

@Service
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerServiceImpl.class);

    private final CustomerRepository customerRepository;
    private final CustomerDocumentRepository documentRepository;
    private final CustomerSpecification<Customer> creationSpecification;

    @Autowired
    public CustomerServiceImpl(
            CustomerRepository customerRepository,
            CustomerDocumentRepository documentRepository,
            List<CustomerSpecification<Customer>> specifications) {
        this.customerRepository = customerRepository;
        this.documentRepository = documentRepository;
        // 按验证顺序组合规约
        this.creationSpecification = specifications.stream()
                .reduce(CustomerSpecification::and)
                .orElseThrow(() -> new IllegalStateException("未配置任何客户验证规约"));
    }

    @Override
    @AuditLoggable(operation = "CREATE_CUSTOMER", entityType = "Customer",
            description = "创建客户 {0.name}")
    public Customer createCustomer(Customer customer, List<MultipartFile> documents) {
        log.info("创建客户，姓名: {}, 身份证号: {}", customer.getName(), customer.getIdCard());
        
        // 1. 执行规约验证链
        SpecificationResult result = creationSpecification.isSatisfiedBy(customer);
        if (!result.isSatisfied()) {
            throw new BusinessException(result.message());
        }

        // 2. 设置默认值
        initializeCustomer(customer);

        // 3. 风险评估
        assessCustomerRisk(customer);

        // 4. 保存客户信息
        Customer savedCustomer = customerRepository.save(customer);
        log.info("客户创建成功，ID: {}", savedCustomer.getId());

        // 5. 处理客户文档
        if (documents != null && !documents.isEmpty()) {
            processCustomerDocuments(savedCustomer, documents);
        }

        return savedCustomer;
    }

    @Override
    @AuditLoggable(operation = "UPDATE_CUSTOMER", entityType = "Customer",
            description = "更新客户 {1}")
    public Customer updateCustomer(Long id, Customer customerDetails, String operator) {
        log.info("更新客户信息，ID: {}, 操作人: {}", id, operator);
        
        Customer existingCustomer = customerRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("客户不存在，ID: " + id));

        // 1. 状态检查（冻结客户不能修改）
        if (CustomerStatus.FROZEN.equals(existingCustomer.getStatus())) {
            log.warn("客户已被冻结，无法修改信息，ID: {}", id);
            throw new BusinessException("客户已被冻结，无法修改信息");
        }

        // 2. 黑名单客户限制
        if (CustomerStatus.BLACKLIST.equals(existingCustomer.getStatus())) {
            // 黑名单客户只能更新部分信息
            validateBlacklistCustomerUpdate(customerDetails);
        }

        // 3. 执行更新
        updateCustomerFields(existingCustomer, customerDetails);

        // 4. 保存更新
        Customer updatedCustomer = customerRepository.save(existingCustomer);

        return updatedCustomer;
    }

    @Override
    public Optional<Customer> getCustomerById(Long id) {
        return customerRepository.findById(id);
    }

    @Override
    public Optional<Customer> getCustomerByIdCard(String idCard) {
        return customerRepository.findByIdCard(idCard);
    }

    @Override
    public Optional<Customer> getCustomerByPhone(String phone) {
        return customerRepository.findByPhone(phone);
    }

    @Override
    public Page<Customer> getCustomerList(String name, String phone, String idCard,
                                         String keyword,
                                         CustomerStatus status, RiskLevel riskLevel,
                                         int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);

        // 动态条件查询
        Specification<Customer> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 关键字搜索：在 name/phone/idCard 中 OR 模糊匹配
            if (keyword != null && !keyword.isEmpty()) {
                String pattern = "%" + keyword + "%";
                predicates.add(cb.or(
                    cb.like(root.get("name"), pattern),
                    cb.like(root.get("phone"), pattern),
                    cb.like(root.get("idCard"), pattern)
                ));
            } else {
                if (name != null && !name.isEmpty()) {
                    predicates.add(cb.like(root.get("name"), "%" + name + "%"));
                }
                if (phone != null && !phone.isEmpty()) {
                    predicates.add(cb.like(root.get("phone"), "%" + phone + "%"));
                }
                if (idCard != null && !idCard.isEmpty()) {
                    predicates.add(cb.like(root.get("idCard"), "%" + idCard + "%"));
                }
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (riskLevel != null) {
                predicates.add(cb.equal(root.get("riskLevel"), riskLevel));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return customerRepository.findAll(spec, pageRequest);
    }

    @Override
    @AuditLoggable(operation = "UPDATE_CUSTOMER_STATUS", entityType = "Customer",
            description = "更新客户状态 {0} → {1}")
    public void updateCustomerStatus(Long id, CustomerStatus status, String reason, String operator) {
        log.info("更新客户状态，ID: {}, 新状态: {}, 操作人: {}", id, status, operator);
        
        Customer customer = customerRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("客户不存在，ID: " + id));

        // 1. 状态流转验证
        validateStatusTransition(customer.getStatus(), status);

        // 2. 执行状态变更
        customer.setStatus(status);
        customer.setStatusReason(reason + " (操作人: " + operator + ")");
        customerRepository.save(customer);

        // 3. 状态变更触发业务
        handleStatusChange(customer, customer.getStatus(), status);

    }

    @Override
    public void updateCreditInfo(Long id, Integer creditScore, String creditReportNo) {
        log.info("更新客户征信信息，ID: {}, 征信分数: {}", id, creditScore);
        
        Customer customer = customerRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("客户不存在，ID: " + id));

        // 1. 征信分数范围验证
        if (creditScore != null && (creditScore < 300 || creditScore > 850)) {
            throw new BusinessException("征信分数必须在300-850范围内");
        }

        // 2. 更新征信信息
        Integer oldCreditScore = customer.getCreditScore();
        customer.setCreditScore(creditScore);
        customer.setCreditReportNo(creditReportNo);
        customerRepository.save(customer);

        // 3. 征信变化触发风险评估
        if (oldCreditScore != null && creditScore != null &&
            Math.abs(oldCreditScore - creditScore) > 50) {
            // 触发风险重评估
            assessCustomerRisk(customer);
        }

    }

    @Override
    public CustomerDocument uploadCustomerDocument(Long customerId, DocumentType documentType,
                                                 MultipartFile file, String operator) {
        // 1. 验证文件类型和大小
        validateDocument(file, documentType);

        // 2. 保存文件到文件系统或云存储
        String filePath = saveDocumentFile(file, customerId, documentType);

        // 3. 创建文档记录
        CustomerDocument document = new CustomerDocument();
        document.setCustomerId(customerId);
        document.setDocumentType(documentType);
        document.setFileName(file.getOriginalFilename());
        document.setFilePath(filePath);
        document.setFileSize(file.getSize());
        document.setUploadTime(LocalDateTime.now());
        document.setUploader(operator);
        document.setStatus("VALID");

        return documentRepository.save(document);
    }

    @Override
    @AuditLoggable(operation = "DELETE_CUSTOMER", entityType = "Customer",
            description = "删除客户 {0}")
    public void deleteCustomer(Long id, String reason, String operator) {
        log.info("删除客户，ID: {}, 原因: {}, 操作人: {}", id, reason, operator);
        
        Customer customer = customerRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("客户不存在，ID: " + id));

        // 1. 检查是否有未结清贷款
        if (hasActiveLoans(id)) {
            throw new BusinessException("客户存在未结清贷款，不能删除");
        }

        // 2. 软删除而非物理删除（合规要求）
        customer.setStatus(CustomerStatus.DELETED);
        customer.setStatusReason("删除原因: " + reason + ", 操作人: " + operator);
        customerRepository.save(customer);

    }

    @Override
    @RequireAdmin
    @AuditLoggable(operation = "BATCH_UPDATE_CUSTOMER_STATUS", entityType = "Customer",
            description = "批量更新 {0.size} 个客户状态为 {1}")
    public void batchUpdateCustomerStatus(List<Long> customerIds, CustomerStatus status,
                                         String reason, String operator) {
        log.info("批量更新客户状态，客户数量: {}, 新状态: {}, 操作人: {}", 
                 customerIds.size(), status, operator);

        // 批量状态更新
        for (Long customerId : customerIds) {
            try {
                updateCustomerStatus(customerId, status, reason, operator);
            } catch (Exception e) {
                log.warn("批量更新失败，客户ID: {}, 原因: {}", customerId, e.getMessage());
            }
        }
    }

    private void initializeCustomer(Customer customer) {
        if (customer.getStatus() == null) {
            customer.setStatus(CustomerStatus.NORMAL);
        }
    }

    private void assessCustomerRisk(Customer customer) {
        // 简化的风险评估逻辑
        if (customer.getCreditScore() != null) {
            if (customer.getCreditScore() >= 700) {
                customer.setRiskLevel(RiskLevel.LOW);
            } else if (customer.getCreditScore() >= 600) {
                customer.setRiskLevel(RiskLevel.MEDIUM);
            } else {
                customer.setRiskLevel(RiskLevel.HIGH);
            }
        }
    }

    private void validateStatusTransition(CustomerStatus currentStatus, CustomerStatus newStatus) {
        // 简化的状态流转验证
        if (CustomerStatus.DELETED.equals(currentStatus)) {
            throw new BusinessException("已删除客户不能修改状态");
        }
    }

    private void handleStatusChange(Customer customer, CustomerStatus oldStatus, CustomerStatus newStatus) {
        // 状态变更后的业务处理
        if (CustomerStatus.BLACKLIST.equals(newStatus)) {
            // 加入黑名单时，自动冻结所有未结清贷款
            freezeAllActiveLoans(customer.getId());
        }
    }

    private void freezeAllActiveLoans(Long customerId) {
        // 冻结客户所有活跃贷款的实现
    }

    private void updateCustomerFields(Customer existing, Customer details) {
        // 只更新允许修改的字段
        if (details.getName() != null) {
            existing.setName(details.getName());
        }

        // 身份证号：仅在非空且与原值不同时更新，需检查唯一性
        if (details.getIdCard() != null && !details.getIdCard().equals(existing.getIdCard())) {
            if (existsByIdCard(details.getIdCard())) {
                throw new BusinessException("新身份证号已存在: " + details.getIdCard());
            }
            existing.setIdCard(details.getIdCard());
        }

        if (details.getPhone() != null && !details.getPhone().equals(existing.getPhone())) {
            if (existsByPhone(details.getPhone())) {
                throw new BusinessException("新手机号已存在: " + details.getPhone());
            }
            existing.setPhone(details.getPhone());
        }

        if (details.getEmail() != null) {
            existing.setEmail(details.getEmail());
        }

        // 更新其他允许修改的字段...
        if (details.getOccupation() != null) {
            existing.setOccupation(details.getOccupation());
        }

        if (details.getMonthlyIncome() != null) {
            existing.setMonthlyIncome(details.getMonthlyIncome());
        }

        if (details.getAddress() != null) {
            existing.setAddress(details.getAddress());
        }
    }

    private void validateBlacklistCustomerUpdate(Customer customerDetails) {
        // 黑名单客户只能更新部分信息
        // 限制更新字段
    }

    private void validateDocument(MultipartFile file, DocumentType documentType) {
        // 验证文件类型和大小的逻辑
    }

    private String saveDocumentFile(MultipartFile file, Long customerId, DocumentType documentType) {
        // 保存文件并返回文件路径
        return "/uploads/" + customerId + "/" + documentType + "/" + file.getOriginalFilename();
    }

    private void processCustomerDocuments(Customer customer, List<MultipartFile> documents) {
        for (MultipartFile document : documents) {
            // 处理每个文档
            DocumentType docType = determineDocumentType(document);
            uploadCustomerDocument(customer.getId(), docType, document, "SYSTEM");
        }
    }

    private DocumentType determineDocumentType(MultipartFile file) {
        // 根据文件名判断文档类型
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return DocumentType.OTHER;
        }

        String lowerCaseName = originalFilename.toLowerCase();
        if (lowerCaseName.contains("身份证") || lowerCaseName.contains("idcard")) {
            return DocumentType.ID_CARD;
        } else if (lowerCaseName.contains("收入") || lowerCaseName.contains("income")) {
            return DocumentType.INCOME_PROOF;
        } else if (lowerCaseName.contains("住址") || lowerCaseName.contains("address")) {
            return DocumentType.ADDRESS_PROOF;
        }

        return DocumentType.OTHER;
    }

    private boolean existsByIdCard(String idCard) {
        return customerRepository.existsByIdCard(idCard);
    }

    private boolean existsByPhone(String phone) {
        return customerRepository.existsByPhone(phone);
    }

    private boolean hasActiveLoans(Long customerId) {
        // 检查客户是否有活跃贷款
        return false;
    }
}
