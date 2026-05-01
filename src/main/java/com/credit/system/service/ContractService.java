package com.credit.system.service;

import com.credit.system.domain.LoanContract;
import com.credit.system.domain.enums.ContractStatus;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ContractService {

    LoanContract createContract(LoanContract contract, String operator);

    Optional<LoanContract> getContractById(Long id);

    Optional<LoanContract> getContractByNo(String contractNo);

    Optional<LoanContract> getContractByApplicationId(Long applicationId);

    List<LoanContract> getContractsByCustomerId(Long customerId);

    Page<LoanContract> getContractList(Long customerId, Long productId,
                                        ContractStatus status, int page, int size);

    LoanContract createContractFromApplication(Long applicationId, String operator);

    void signContract(Long id, String signatory, String signatureMethod);

    void terminateContract(Long id, String reason, String operator);

    void extendContract(Long id, int months, String reason, String operator);

    void settleContract(Long id);

    List<LoanContract> getOverdueContracts();

    List<LoanContract> getContractsDueBetween(LocalDate from, LocalDate to);
}
