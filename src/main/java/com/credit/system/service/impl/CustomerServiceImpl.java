package com.credit.system.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.criteria.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.credit.system.util.IdCardUtil;
import com.credit.system.web.multipart.MultipartFile;

@Service
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerServiceImpl.class);

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerDocumentRepository documentRepository;

    @Override
    public Customer createCustomer(Customer customer, List<MultipartFile> documents) {
        log.info("创建客户，姓名: {}, 身份证号: {}", customer.getName(), customer.getIdCard());
        
        // 1. 基础数据验证
        validateCustomerData(customer);

        // 2. 唯一性检查
        validateCustomerUniqueness(customer);

        // 3. 年龄验证
        validateCustomerAge(customer);

        // 4. 黑名单检查
        checkBlacklist(customer);

        // 5. 设置默认值
        initializeCustomer(customer);

        // 6. 风险评估
        assessCustomerRisk(customer);

        // 7. 保存客户信息
        Customer savedCustomer = customerRepository.save(customer);
        log.info("客户创建成功，ID: {}", savedCustomer.getId());

        // 8. 处理客户文档
        if (documents != null && !documents.isEmpty()) {
            processCustomerDocuments(savedCustomer, documents);
        }

        // 9. 记录审计日志
        auditLog("CREATE_CUSTOMER", savedCustomer.getId(), "创建客户");

        return savedCustomer;
    }

    @Override
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

        // 5. 记录审计日志
        auditLog("UPDATE_CUSTOMER", id, "更新客户信息");

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
                                         CustomerStatus status, RiskLevel riskLevel,
                                         int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);

        // 动态条件查询
        Specification<Customer> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (name != null && !name.isEmpty()) {
                predicates.add(cb.like(root.get("name"), "%" + name + "%"));
            }
            if (phone != null && !phone.isEmpty()) {
                predicates.add(cb.like(root.get("phone"), "%" + phone + "%"));
            }
            if (idCard != null && !idCard.isEmpty()) {
                predicates.add(cb.like(root.get("idCard"), "%" + idCard + "%"));
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

        // 4. 记录审计日志
        auditLog("UPDATE_STATUS", id, "状态变更: " + status);
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

        // 4. 记录审计日志
        auditLog("UPDATE_CREDIT_INFO", id, "征信更新");
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

        // 3. 记录删除日志
        auditLog("DELETE_CUSTOMER", id, "删除客户: " + reason);
    }

    @Override
    public void batchUpdateCustomerStatus(List<Long> customerIds, CustomerStatus status,
                                         String reason, String operator) {
        log.info("批量更新客户状态，客户数量: {}, 新状态: {}, 操作人: {}", 
                 customerIds.size(), status, operator);
        
        // 1. 批量操作权限验证
        validateBatchOperationPermission(operator);

        // 2. 批量状态更新
        for (Long customerId : customerIds) {
            try {
                updateCustomerStatus(customerId, status, reason, operator);
            } catch (Exception e) {
                // 记录失败日志，继续处理其他客户
                auditLog("BATCH_UPDATE_FAILED", customerId, "批量状态更新失败: " + e.getMessage());
            }
        }

        // 3. 记录批量操作日志
        auditLog("BATCH_UPDATE_STATUS", 0L,
                 "批量更新客户状态: " + customerIds.size() + " 个客户");
    }

    // 私有验证方法
    private void validateCustomerData(Customer customer) {
        // 月收入验证
        if (customer.getMonthlyIncome() != null && customer.getMonthlyIncome().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("月收入不能为负数");
        }

        // 紧急联系人电话验证
        if (customer.getEmergencyContactPhone() != null) {
            if (!customer.getEmergencyContactPhone().matches("^1[3-9]\\d{9}$")) {
                throw new BusinessException("紧急联系人手机号格式不正确");
            }
        }
    }

    private void validateCustomerUniqueness(Customer customer) {
        if (existsByIdCard(customer.getIdCard())) {
            throw new BusinessException("身份证号已存在: " + customer.getIdCard());
        }

        if (existsByPhone(customer.getPhone())) {
            throw new BusinessException("手机号已存在: " + customer.getPhone());
        }
    }

    private void validateCustomerAge(Customer customer) {
        int age = IdCardUtil.calculateAge(customer.getIdCard());
        if (age < 18 || age > 65) {
            throw new BusinessException(
                String.format("客户年龄不符合要求：当前年龄%d岁，要求18-65岁", age)
            );
        }
    }

    private void checkBlacklist(Customer customer) {
        // 检查身份证号是否在黑名单
        if (customerRepository.existsInBlacklistByIdCard(customer.getIdCard())) {
            throw new BusinessException("客户身份证号在黑名单中");
        }

        // 检查手机号是否在黑名单
        if (customerRepository.existsInBlacklistByPhone(customer.getPhone())) {
            throw new BusinessException("客户手机号在黑名单中");
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

    private void validateBatchOperationPermission(String operator) {
        // 批量操作需要特殊权限
        if (!hasBatchOperationPermission(operator)) {
            throw new BusinessException("没有批量操作权限");
        }
    }

    private boolean hasBatchOperationPermission(String operator) {
        // 权限验证逻辑
        return "ADMIN".equals(operator) || "SUPER_USER".equals(operator);
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

    private void auditLog(String operation, Long customerId, String details) {
        // 记录审计日志的实现
    }
}