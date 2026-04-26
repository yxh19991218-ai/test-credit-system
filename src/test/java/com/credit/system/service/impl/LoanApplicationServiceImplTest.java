package com.credit.system.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
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
import com.credit.system.domain.LoanProduct;
import com.credit.system.domain.RepaymentSchedule;
import com.credit.system.domain.enums.ApplicationStatus;
import com.credit.system.domain.enums.ProductStatus;
import com.credit.system.exception.BusinessException;
import com.credit.system.exception.ResourceNotFoundException;
import com.credit.system.repository.CustomerRepository;
import com.credit.system.repository.LoanApplicationRepository;
import com.credit.system.repository.LoanContractRepository;
import com.credit.system.repository.LoanProductRepository;
import com.credit.system.service.RepaymentScheduleService;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoanApplicationServiceImpl 单元测试")
class LoanApplicationServiceImplTest {

    @Mock
    private LoanApplicationRepository applicationRepository;

    @Mock
    private LoanProductRepository productRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private LoanContractRepository contractRepository;

    @Mock
    private RepaymentScheduleService scheduleService;

    @InjectMocks
    private LoanApplicationServiceImpl applicationService;

    private LoanApplication sampleApplication;
    private Customer sampleCustomer;
    private LoanProduct sampleProduct;

    @BeforeEach
    void setUp() {
        sampleCustomer = new Customer();
        sampleCustomer.setId(1L);
        sampleCustomer.setName("张三");
        sampleCustomer.setCreditScore(700);
        sampleCustomer.setMonthlyIncome(new BigDecimal("15000"));

        sampleProduct = new LoanProduct();
        sampleProduct.setId(1L);
        sampleProduct.setProductCode("P001");
        sampleProduct.setStatus(ProductStatus.ACTIVE);
        sampleProduct.setMinAmount(new BigDecimal("5000"));
        sampleProduct.setMaxAmount(new BigDecimal("200000"));
        sampleProduct.setMinTerm(3);
        sampleProduct.setMaxTerm(36);
        sampleProduct.setMinCreditScore(600);
        sampleProduct.setMinMonthlyIncome(new BigDecimal("5000"));

        sampleApplication = new LoanApplication();
        sampleApplication.setId(1L);
        sampleApplication.setCustomerId(1L);
        sampleApplication.setProductId(1L);
        sampleApplication.setApplyAmount(new BigDecimal("100000"));
        sampleApplication.setApplyTerm(12);
        sampleApplication.setPurpose("装修");
        sampleApplication.setStatus(ApplicationStatus.DRAFT);
        sampleApplication.setApplicationDate(LocalDateTime.now());
    }

    @Nested
    @DisplayName("创建申请 createApplication")
    class CreateApplication {

        @Test
        @DisplayName("应成功创建申请")
        void shouldCreateSuccessfully() {
            given(customerRepository.findById(1L)).willReturn(Optional.of(sampleCustomer));
            given(productRepository.findById(1L)).willReturn(Optional.of(sampleProduct));
            given(applicationRepository.save(any(LoanApplication.class))).willReturn(sampleApplication);

            LoanApplication result = applicationService.createApplication(sampleApplication, "SYSTEM");

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(ApplicationStatus.DRAFT);
            verify(applicationRepository, times(1)).save(any(LoanApplication.class));
        }

        @Test
        @DisplayName("客户不存在应抛出异常")
        void shouldThrowExceptionWhenCustomerNotFound() {
            given(customerRepository.findById(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> applicationService.createApplication(sampleApplication, "SYSTEM"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("客户不存在");
        }

        @Test
        @DisplayName("产品不存在应抛出异常")
        void shouldThrowExceptionWhenProductNotFound() {
            given(customerRepository.findById(1L)).willReturn(Optional.of(sampleCustomer));
            given(productRepository.findById(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> applicationService.createApplication(sampleApplication, "SYSTEM"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("产品不存在");
        }

        @Test
        @DisplayName("产品未上架应抛出异常")
        void shouldThrowExceptionWhenProductNotActive() {
            sampleProduct.setStatus(ProductStatus.DRAFT);
            given(customerRepository.findById(1L)).willReturn(Optional.of(sampleCustomer));
            given(productRepository.findById(1L)).willReturn(Optional.of(sampleProduct));

            assertThatThrownBy(() -> applicationService.createApplication(sampleApplication, "SYSTEM"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("产品未上架");
        }

        @Test
        @DisplayName("金额超出产品范围应抛出异常")
        void shouldThrowExceptionWhenAmountOutOfRange() {
            sampleApplication.setApplyAmount(new BigDecimal("500000"));
            given(customerRepository.findById(1L)).willReturn(Optional.of(sampleCustomer));
            given(productRepository.findById(1L)).willReturn(Optional.of(sampleProduct));

            assertThatThrownBy(() -> applicationService.createApplication(sampleApplication, "SYSTEM"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("申请金额必须在");
        }

        @Test
        @DisplayName("期限超出产品范围应抛出异常")
        void shouldThrowExceptionWhenTermOutOfRange() {
            sampleApplication.setApplyTerm(60);
            given(customerRepository.findById(1L)).willReturn(Optional.of(sampleCustomer));
            given(productRepository.findById(1L)).willReturn(Optional.of(sampleProduct));

            assertThatThrownBy(() -> applicationService.createApplication(sampleApplication, "SYSTEM"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("申请期限必须在");
        }

        @Test
        @DisplayName("征信分数不满足应抛出异常")
        void shouldThrowExceptionWhenCreditScoreLow() {
            sampleCustomer.setCreditScore(500);
            given(customerRepository.findById(1L)).willReturn(Optional.of(sampleCustomer));
            given(productRepository.findById(1L)).willReturn(Optional.of(sampleProduct));

            assertThatThrownBy(() -> applicationService.createApplication(sampleApplication, "SYSTEM"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("征信分数不满足");
        }

        @Test
        @DisplayName("月收入不满足应抛出异常")
        void shouldThrowExceptionWhenIncomeLow() {
            sampleCustomer.setMonthlyIncome(new BigDecimal("3000"));
            given(customerRepository.findById(1L)).willReturn(Optional.of(sampleCustomer));
            given(productRepository.findById(1L)).willReturn(Optional.of(sampleProduct));

            assertThatThrownBy(() -> applicationService.createApplication(sampleApplication, "SYSTEM"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("月收入不满足");
        }
    }

    @Nested
    @DisplayName("更新申请 updateApplication")
    class UpdateApplication {

        @Test
        @DisplayName("应成功更新草稿申请")
        void shouldUpdateDraftSuccessfully() {
            given(applicationRepository.findById(1L)).willReturn(Optional.of(sampleApplication));
            given(applicationRepository.save(any(LoanApplication.class))).willAnswer(invocation -> invocation.getArgument(0));

            LoanApplication details = new LoanApplication();
            details.setApplyAmount(new BigDecimal("80000"));
            details.setApplyTerm(24);
            details.setPurpose("购车");

            LoanApplication result = applicationService.updateApplication(1L, details);

            assertThat(result.getApplyAmount()).isEqualByComparingTo(new BigDecimal("80000"));
            assertThat(result.getApplyTerm()).isEqualTo(24);
            assertThat(result.getPurpose()).isEqualTo("购车");
        }

        @Test
        @DisplayName("非草稿状态不能修改")
        void shouldNotUpdateNonDraft() {
            sampleApplication.setStatus(ApplicationStatus.PENDING);
            given(applicationRepository.findById(1L)).willReturn(Optional.of(sampleApplication));

            assertThatThrownBy(() -> applicationService.updateApplication(1L, new LoanApplication()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("仅草稿状态的申请可以修改");
        }
    }

    @Nested
    @DisplayName("提交申请 submitApplication")
    class SubmitApplication {

        @Test
        @DisplayName("应成功提交草稿申请")
        void shouldSubmitDraftSuccessfully() {
            given(applicationRepository.findById(1L)).willReturn(Optional.of(sampleApplication));
            given(applicationRepository.save(any(LoanApplication.class))).willAnswer(invocation -> invocation.getArgument(0));

            applicationService.submitApplication(1L);

            assertThat(sampleApplication.getStatus()).isEqualTo(ApplicationStatus.PENDING);
        }

        @Test
        @DisplayName("非草稿状态不能提交")
        void shouldNotSubmitNonDraft() {
            sampleApplication.setStatus(ApplicationStatus.PENDING);
            given(applicationRepository.findById(1L)).willReturn(Optional.of(sampleApplication));

            assertThatThrownBy(() -> applicationService.submitApplication(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("仅草稿状态的申请可以提交");
        }
    }

    @Nested
    @DisplayName("审核申请 reviewApplication")
    class ReviewApplication {

        @Test
        @DisplayName("审批通过应更新状态和额度")
        void shouldApproveSuccessfully() {
            sampleApplication.setStatus(ApplicationStatus.PENDING);
            given(applicationRepository.findById(1L)).willReturn(Optional.of(sampleApplication));
            given(applicationRepository.save(any(LoanApplication.class))).willAnswer(invocation -> invocation.getArgument(0));

            applicationService.reviewApplication(1L, ApplicationStatus.APPROVED, "审核员",
                    "信用良好", new BigDecimal("100000"), 12, new BigDecimal("0.12"));

            assertThat(sampleApplication.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
            assertThat(sampleApplication.getApprovedAmount()).isEqualByComparingTo(new BigDecimal("100000"));
            assertThat(sampleApplication.getReviewer()).isEqualTo("审核员");
        }

        @Test
        @DisplayName("驳回应更新状态")
        void shouldRejectSuccessfully() {
            sampleApplication.setStatus(ApplicationStatus.PENDING);
            given(applicationRepository.findById(1L)).willReturn(Optional.of(sampleApplication));
            given(applicationRepository.save(any(LoanApplication.class))).willAnswer(invocation -> invocation.getArgument(0));

            applicationService.reviewApplication(1L, ApplicationStatus.REJECTED, "审核员",
                    "资料不足", null, null, null);

            assertThat(sampleApplication.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
        }

        @Test
        @DisplayName("非待审批状态不能审核")
        void shouldNotReviewNonPending() {
            given(applicationRepository.findById(1L)).willReturn(Optional.of(sampleApplication));

            assertThatThrownBy(() -> applicationService.reviewApplication(1L, ApplicationStatus.APPROVED,
                    "审核员", "ok", null, null, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("仅待审批状态的申请可以审核");
        }
    }

    @Nested
    @DisplayName("取消申请 cancelApplication")
    class CancelApplication {

        @Test
        @DisplayName("应成功取消申请")
        void shouldCancelSuccessfully() {
            sampleApplication.setStatus(ApplicationStatus.PENDING);
            given(applicationRepository.findById(1L)).willReturn(Optional.of(sampleApplication));
            given(applicationRepository.save(any(LoanApplication.class))).willAnswer(invocation -> invocation.getArgument(0));

            applicationService.cancelApplication(1L, "用户主动取消");

            assertThat(sampleApplication.getStatus()).isEqualTo(ApplicationStatus.CANCELLED);
        }

        @Test
        @DisplayName("已完成申请不能取消")
        void shouldNotCancelCompleted() {
            sampleApplication.setStatus(ApplicationStatus.COMPLETED);
            given(applicationRepository.findById(1L)).willReturn(Optional.of(sampleApplication));

            assertThatThrownBy(() -> applicationService.cancelApplication(1L, "test"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("已完成申请不能取消");
        }
    }

    @Nested
    @DisplayName("审批转合同 approveToContract")
    class ApproveToContract {

        @Test
        @DisplayName("应成功完成申请并生成还款计划")
        void shouldApproveToContractSuccessfully() {
            sampleApplication.setStatus(ApplicationStatus.APPROVED);
            LoanContract contract = new LoanContract();
            contract.setId(1L);

            given(applicationRepository.findById(1L)).willReturn(Optional.of(sampleApplication));
            given(contractRepository.findById(1L)).willReturn(Optional.of(contract));
            given(scheduleService.generateSchedule(1L)).willReturn(new RepaymentSchedule());
            given(applicationRepository.save(any(LoanApplication.class))).willAnswer(invocation -> invocation.getArgument(0));

            applicationService.approveToContract(1L, 1L);

            assertThat(sampleApplication.getStatus()).isEqualTo(ApplicationStatus.COMPLETED);
            assertThat(sampleApplication.getContractId()).isEqualTo(1L);
            verify(scheduleService, times(1)).generateSchedule(1L);
        }

        @Test
        @DisplayName("非已审批状态不能转合同")
        void shouldNotApproveNonApproved() {
            LoanContract contract = new LoanContract();
            contract.setId(1L);
            given(applicationRepository.findById(1L)).willReturn(Optional.of(sampleApplication));
            given(contractRepository.findById(1L)).willReturn(Optional.of(contract));

            assertThatThrownBy(() -> applicationService.approveToContract(1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("仅已审批的申请可以生成合同");
        }
    }

    @Nested
    @DisplayName("查询申请")
    class GetApplication {

        @Test
        @DisplayName("按ID查询应返回申请")
        void shouldReturnById() {
            given(applicationRepository.findById(1L)).willReturn(Optional.of(sampleApplication));

            Optional<LoanApplication> result = applicationService.getApplicationById(1L);

            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("按合同ID查询应返回申请")
        void shouldReturnByContractId() {
            given(applicationRepository.findByContractId(1L)).willReturn(Optional.of(sampleApplication));

            Optional<LoanApplication> result = applicationService.getApplicationByContractId(1L);

            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("按客户ID分页查询应返回结果")
        void shouldReturnByCustomerId() {
            Page<LoanApplication> page = new PageImpl<>(Collections.singletonList(sampleApplication));
            given(applicationRepository.findByCustomerId(eq(1L), any(PageRequest.class))).willReturn(page);

            Page<LoanApplication> result = applicationService.getApplicationsByCustomerId(1L, 0, 10);

            assertThat(result.getContent()).hasSize(1);
        }
    }
}
