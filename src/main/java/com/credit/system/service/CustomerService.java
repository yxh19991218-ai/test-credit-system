package com.credit.system.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;

import com.credit.system.domain.Customer;
import com.credit.system.domain.CustomerDocument;
import com.credit.system.domain.enums.CustomerStatus;
import com.credit.system.domain.enums.DocumentType;
import com.credit.system.domain.enums.RiskLevel;
import com.credit.system.web.multipart.MultipartFile;

public interface CustomerService {

    Customer createCustomer(Customer customer, List<MultipartFile> documents);

    Customer updateCustomer(Long id, Customer customerDetails, String operator);

    Optional<Customer> getCustomerById(Long id);

    Optional<Customer> getCustomerByIdCard(String idCard);

    Optional<Customer> getCustomerByPhone(String phone);

    Page<Customer> getCustomerList(String name, String phone, String idCard,
                                   String keyword,
                                   CustomerStatus status, RiskLevel riskLevel,
                                   int page, int size);

    void updateCustomerStatus(Long id, CustomerStatus status, String reason, String operator);

    void updateCreditInfo(Long id, Integer creditScore, String creditReportNo);

    CustomerDocument uploadCustomerDocument(Long customerId, DocumentType documentType,
                                            MultipartFile file, String operator);

    void deleteCustomer(Long id, String reason, String operator);

    void batchUpdateCustomerStatus(List<Long> customerIds, CustomerStatus status,
                                   String reason, String operator);
}
