package com.credit.system.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import com.credit.system.domain.Customer;
import com.credit.system.domain.LoanApplication;
import com.credit.system.domain.LoanContract;
import com.credit.system.domain.enums.ApplicationStatus;
import com.credit.system.domain.enums.ContractStatus;
import com.credit.system.exception.BusinessException;
import com.credit.system.exception.ResourceNotFoundException;
import com.credit.system.repository.CustomerRepository;
import com.credit.system.repository.LoanApplicationRepository;
import com.credit.system.repository.LoanContractRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContractServiceImpl 单元测试")
class ContractServiceImplTest {

    @Mock
    private LoanContractRepository contractRepository;

    @Mock
    private LoanApplicationRepository applicationRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private ContractServiceImpl contractService;

    private LoanContract sampleContract;
    private Customer sampleCustomer;
    private LoanApplication sampleApplication;

    @BeforeEach
    void setUp() {
        sampleCustomer = new Customer();
        sampleCustomer.setId(1L);
        sampleCustomer.setName("张三");

        sampleApplication = new LoanApplication();
        sampleApplication.setId(1L);
        sampleApplication.setCustomerId(1L);
        sampleApplication.setStatus(ApplicationStatus.APPROVED);

        sampleContract = new LoanContract();
        sampleContract.setId(1L);
        sampleContract.setCustomerId(1L);
        sampleContract.setApplicationId(1L);
        sampleContract.setTotalAmount(new BigDecimal("100000"));
        sampleContract.setInterestRate(new BigDecimal("0.12"));
        sampleContract.setTerm(12);
        sampleContract.setStartDate(LocalDate.now());
        sampleContract.setEndDate(LocalDate.now().plusMonths(12));
        sampleContract.setStatus(ContractStatus.ACTIVE);
        sampleContract.setRemainingPrincipal(new BigDecimal("100000"));
        sampleContract.setPaidPeriods(0);
        sampleContract.setExtendedMonths(0);
    }

    @Nested
    @DisplayName("创建合同 createContract")
    class CreateContract {

        @Test
        @DisplayName("应成功创建合同")
        void shouldCreateSuccessfully() {
            given(customerRepository.findById(1L)).willReturn(Optional.of(sampleCustomer));
            given(applicationRepository.findById(1L)).willReturn(Optional.of(sampleApplication));
            given(contractRepository.existsByContractNo(anyString())).willReturn(false);
            given(contractRepository.save(any(LoanContract.class))).willReturn(sampleContract);

            LoanContract result = contractService.createContract(sampleContract, "SYSTEM");

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(ContractStatus.ACTIVE);
            assertThat(result.getContractNo()).isNotNull();
            verify(contractRepository, times(1)).save(any(LoanContract.class));
        }

        @Test
        @DisplayName("客户不存在应抛出异常")
        void shouldThrowExceptionWhenCustomerNotFound() {
            given(customerRepository.findById(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> contractService.createContract(sampleContract, "SYSTEM"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("客户不存在");
        }

        @Test
        @DisplayName("申请状态不是已通过应抛出异常")
        void shouldThrowExceptionWhenApplicationNotApproved() {
            sampleApplication.setStatus(ApplicationStatus.DRAFT);
            given(customerRepository.findById(1L)).willReturn(Optional.of(sampleCustomer));
            given(applicationRepository.findById(1L)).willReturn(Optional.of(sampleApplication));

            assertThatThrownBy(() -> contractService.createContract(sampleContract, "SYSTEM"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("申请状态不是已通过");
        }

        @Test
        @DisplayName("客户与申请不匹配应抛出异常")
        void shouldThrowExceptionWhenCustomerMismatch() {
            sampleApplication.setCustomerId(2L);
            given(customerRepository.findById(1L)).willReturn(Optional.of(sampleCustomer));
            given(applicationRepository.findById(1L)).willReturn(Optional.of(sampleApplication));

            assertThatThrownBy(() -> contractService.createContract(sampleContract, "SYSTEM"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("申请与客户不匹配");
        }
    }

    @Nested
    @DisplayName("签署合同 signContract")
    class SignContract {

        @Test
        @DisplayName("应成功签署合同")
        void shouldSignSuccessfully() {
            given(contractRepository.findById(1L)).willReturn(Optional.of(sampleContract));
            given(contractRepository.save(any(LoanContract.class))).willAnswer(invocation -> invocation.getArgument(0));

            contractService.signContract(1L, "张三", "ONLINE");

            assertThat(sampleContract.getSignDate()).isNotNull();
            assertThat(sampleContract.getSignatory()).isEqualTo("张三");
            assertThat(sampleContract.getSignatureMethod()).isEqualTo("ONLINE");
        }

        @Test
        @DisplayName("非活跃合同不能签署")
        void shouldNotSignNonActiveContract() {
            sampleContract.setStatus(ContractStatus.SETTLED);
            given(contractRepository.findById(1L)).willReturn(Optional.of(sampleContract));

            assertThatThrownBy(() -> contractService.signContract(1L, "张三", "ONLINE"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("合同状态异常");
        }
    }

    @Nested
    @DisplayName("终止合同 terminateContract")
    class TerminateContract {

        @Test
        @DisplayName("应成功终止合同")
        void shouldTerminateSuccessfully() {
            given(contractRepository.findById(1L)).willReturn(Optional.of(sampleContract));

            contractService.terminateContract(1L, "违约", "ADMIN");

            verify(contractRepository, times(1)).terminateContract(eq(1L), eq(ContractStatus.BAD_DEBT), eq("违约"));
        }

        @Test
        @DisplayName("已结清合同不能终止")
        void shouldNotTerminateSettled() {
            sampleContract.setStatus(ContractStatus.SETTLED);
            given(contractRepository.findById(1L)).willReturn(Optional.of(sampleContract));

            assertThatThrownBy(() -> contractService.terminateContract(1L, "test", "ADMIN"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("已结清合同不能终止");
        }
    }

    @Nested
    @DisplayName("合同展期 extendContract")
    class ExtendContract {

        @Test
        @DisplayName("应成功展期合同")
        void shouldExtendSuccessfully() {
            given(contractRepository.findById(1L)).willReturn(Optional.of(sampleContract));

            contractService.extendContract(1L, 3, "资金周转困难", "ADMIN");

            verify(contractRepository, times(1)).extendContract(eq(1L), eq(3), eq("资金周转困难"), any(LocalDate.class));
        }

        @Test
        @DisplayName("非活跃合同不能展期")
        void shouldNotExtendNonActive() {
            sampleContract.setStatus(ContractStatus.SETTLED);
            given(contractRepository.findById(1L)).willReturn(Optional.of(sampleContract));

            assertThatThrownBy(() -> contractService.extendContract(1L, 3, "test", "ADMIN"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("仅活跃合同可以展期");
        }

        @Test
        @DisplayName("累计展期超过12个月应抛出异常")
        void shouldNotExtendBeyond12Months() {
            sampleContract.setExtendedMonths(10);
            given(contractRepository.findById(1L)).willReturn(Optional.of(sampleContract));

            assertThatThrownBy(() -> contractService.extendContract(1L, 3, "test", "ADMIN"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("累计展期不能超过12个月");
        }
    }

    @Nested
    @DisplayName("结清合同 settleContract")
    class SettleContract {

        @Test
        @DisplayName("应成功结清合同")
        void shouldSettleSuccessfully() {
            given(contractRepository.findById(1L)).willReturn(Optional.of(sampleContract));
            given(contractRepository.save(any(LoanContract.class))).willAnswer(invocation -> invocation.getArgument(0));

            contractService.settleContract(1L);

            assertThat(sampleContract.getStatus()).isEqualTo(ContractStatus.SETTLED);
            assertThat(sampleContract.getRemainingPrincipal()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("非活跃合同不能结清")
        void shouldNotSettleNonActive() {
            sampleContract.setStatus(ContractStatus.SETTLED);
            given(contractRepository.findById(1L)).willReturn(Optional.of(sampleContract));

            assertThatThrownBy(() -> contractService.settleContract(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("仅活跃合同可以结清");
        }
    }

    @Nested
    @DisplayName("查询合同")
    class GetContract {

        @Test
        @DisplayName("按ID查询应返回合同")
        void shouldReturnById() {
            given(contractRepository.findById(1L)).willReturn(Optional.of(sampleContract));

            Optional<LoanContract> result = contractService.getContractById(1L);

            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("按合同号查询应返回合同")
        void shouldReturnByNo() {
            given(contractRepository.findByContractNo("LOAN20260426001")).willReturn(Optional.of(sampleContract));

            Optional<LoanContract> result = contractService.getContractByNo("LOAN20260426001");

            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("按申请ID查询应返回合同")
        void shouldReturnByApplicationId() {
            given(contractRepository.findByApplicationId(1L)).willReturn(Optional.of(sampleContract));

            Optional<LoanContract> result = contractService.getContractByApplicationId(1L);

            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("按客户ID查询应返回合同列表")
        void shouldReturnByCustomerId() {
            given(contractRepository.findByCustomerId(1L)).willReturn(Collections.singletonList(sampleContract));

            List<LoanContract> result = contractService.getContractsByCustomerId(1L);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("分页查询应返回结果")
        void shouldReturnPagedResults() {
            Page<LoanContract> page = new PageImpl<>(Collections.singletonList(sampleContract));
            given(contractRepository.findAll(any(PageRequest.class))).willReturn(page);

            Page<LoanContract> result = contractService.getContractList(null, null, null, 0, 10);

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("按客户ID分页查询应返回结果")
        void shouldReturnPagedByCustomerId() {
            Page<LoanContract> page = new PageImpl<>(Collections.singletonList(sampleContract));
            given(contractRepository.findByCustomerId(eq(1L), any(PageRequest.class))).willReturn(page);

            Page<LoanContract> result = contractService.getContractList(1L, null, null, 0, 10);

            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("逾期和到期查询")
    class OverdueAndDue {

        @Test
        @DisplayName("查询逾期合同应返回列表")
        void shouldReturnOverdueContracts() {
            given(contractRepository.findOverdueContracts(any(LocalDate.class)))
                    .willReturn(Collections.singletonList(sampleContract));

            List<LoanContract> result = contractService.getOverdueContracts();

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("查询指定日期范围到期合同应返回列表")
        void shouldReturnContractsDueBetween() {
            given(contractRepository.findContractsDueBetween(any(LocalDate.class), any(LocalDate.class)))
                    .willReturn(Collections.singletonList(sampleContract));

            List<LoanContract> result = contractService.getContractsDueBetween(LocalDate.now(), LocalDate.now().plusDays(30));

            assertThat(result).hasSize(1);
        }
    }
}
