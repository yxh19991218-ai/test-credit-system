package com.credit.system.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import com.credit.system.domain.Customer;
import com.credit.system.domain.LoanApplication;
import com.credit.system.domain.LoanProduct;
import com.credit.system.domain.RepaymentPeriod;
import com.credit.system.domain.enums.ApplicationStatus;
import com.credit.system.domain.enums.ProductStatus;
import com.credit.system.domain.enums.RepaymentMethod;
import com.credit.system.event.ApplicationApprovedEvent;
import com.credit.system.event.EventBus;
import com.credit.system.exception.BusinessException;
import com.credit.system.exception.ResourceNotFoundException;
import com.credit.system.repository.CustomerRepository;
import com.credit.system.repository.LoanApplicationRepository;
import com.credit.system.repository.LoanProductRepository;
import com.credit.system.service.calculator.RepaymentCalculator;
import com.credit.system.service.calculator.RepaymentCalculatorRegistry;

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
    private EventBus eventBus;

    @Mock
    private RepaymentCalculatorRegistry calculatorRegistry;

    @Mock
    private RepaymentCalculator repaymentCalculator;

    @Captor
    private ArgumentCaptor<ApplicationApprovedEvent> eventCaptor;

    private LoanApplicationServiceImpl applicationService;

    private LoanApplication sampleApplication;
    private Customer sampleCustomer;
    private LoanProduct sampleProduct;

    @BeforeEach
    void setUp() {
        applicationService = new LoanApplicationServiceImpl(
                applicationRepository, productRepository, customerRepository, eventBus, calculatorRegistry);

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
            assertThat(result.getApplicationDate()).isNotNull();
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
        @DisplayName("金额等于产品边界值应成功创建")
        void shouldCreateSuccessfullyWhenAmountAtBoundary() {
            sampleApplication.setApplyAmount(new BigDecimal("5000"));
            given(customerRepository.findById(1L)).willReturn(Optional.of(sampleCustomer));
            given(productRepository.findById(1L)).willReturn(Optional.of(sampleProduct));
            given(applicationRepository.save(any(LoanApplication.class))).willReturn(sampleApplication);

            LoanApplication result = applicationService.createApplication(sampleApplication, "SYSTEM");

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(ApplicationStatus.DRAFT);
        }

        @Test
        @DisplayName("金额等于产品最大值应成功创建")
        void shouldCreateSuccessfullyWhenAmountAtMaxBoundary() {
            sampleApplication.setApplyAmount(new BigDecimal("200000"));
            given(customerRepository.findById(1L)).willReturn(Optional.of(sampleCustomer));
            given(productRepository.findById(1L)).willReturn(Optional.of(sampleProduct));
            given(applicationRepository.save(any(LoanApplication.class))).willReturn(sampleApplication);

            LoanApplication result = applicationService.createApplication(sampleApplication, "SYSTEM");

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(ApplicationStatus.DRAFT);
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
        @DisplayName("期限等于产品边界值应成功创建")
        void shouldCreateSuccessfullyWhenTermAtBoundary() {
            sampleApplication.setApplyTerm(3);
            given(customerRepository.findById(1L)).willReturn(Optional.of(sampleCustomer));
            given(productRepository.findById(1L)).willReturn(Optional.of(sampleProduct));
            given(applicationRepository.save(any(LoanApplication.class))).willReturn(sampleApplication);

            LoanApplication result = applicationService.createApplication(sampleApplication, "SYSTEM");

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(ApplicationStatus.DRAFT);
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
        @DisplayName("客户征信分数为null且产品要求最低分数应抛出异常")
        void shouldThrowExceptionWhenCreditScoreIsNull() {
            sampleCustomer.setCreditScore(null);
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

        @Test
        @DisplayName("客户月收入为null且产品要求最低收入应抛出异常")
        void shouldThrowExceptionWhenMonthlyIncomeIsNull() {
            sampleCustomer.setMonthlyIncome(null);
            given(customerRepository.findById(1L)).willReturn(Optional.of(sampleCustomer));
            given(productRepository.findById(1L)).willReturn(Optional.of(sampleProduct));

            assertThatThrownBy(() -> applicationService.createApplication(sampleApplication, "SYSTEM"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("月收入不满足");
        }

        @Test
        @DisplayName("产品不要求最低征信分数时跳过校验")
        void shouldSkipCreditScoreCheckWhenProductHasNoMinCreditScore() {
            sampleProduct.setMinCreditScore(null);
            sampleCustomer.setCreditScore(null);
            given(customerRepository.findById(1L)).willReturn(Optional.of(sampleCustomer));
            given(productRepository.findById(1L)).willReturn(Optional.of(sampleProduct));
            given(applicationRepository.save(any(LoanApplication.class))).willReturn(sampleApplication);

            LoanApplication result = applicationService.createApplication(sampleApplication, "SYSTEM");

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(ApplicationStatus.DRAFT);
        }

        @Test
        @DisplayName("产品不要求最低月收入时跳过校验")
        void shouldSkipIncomeCheckWhenProductHasNoMinIncome() {
            sampleProduct.setMinMonthlyIncome(null);
            sampleCustomer.setMonthlyIncome(null);
            given(customerRepository.findById(1L)).willReturn(Optional.of(sampleCustomer));
            given(productRepository.findById(1L)).willReturn(Optional.of(sampleProduct));
            given(applicationRepository.save(any(LoanApplication.class))).willReturn(sampleApplication);

            LoanApplication result = applicationService.createApplication(sampleApplication, "SYSTEM");

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(ApplicationStatus.DRAFT);
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

        @Test
        @DisplayName("申请不存在应抛出异常")
        void shouldThrowExceptionWhenNotFound() {
            given(applicationRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> applicationService.updateApplication(99L, new LoanApplication()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("申请不存在");
        }

        @Test
        @DisplayName("只更新非空字段，空字段不覆盖原有值")
        void shouldOnlyUpdateNonNullFields() {
            sampleApplication.setApplyAmount(new BigDecimal("100000"));
            sampleApplication.setApplyTerm(12);
            sampleApplication.setPurpose("装修");
            given(applicationRepository.findById(1L)).willReturn(Optional.of(sampleApplication));
            given(applicationRepository.save(any(LoanApplication.class))).willAnswer(invocation -> invocation.getArgument(0));

            LoanApplication details = new LoanApplication();
            details.setApplyAmount(new BigDecimal("80000"));
            // applyTerm 和 purpose 为 null，不应覆盖

            LoanApplication result = applicationService.updateApplication(1L, details);

            assertThat(result.getApplyAmount()).isEqualByComparingTo(new BigDecimal("80000"));
            assertThat(result.getApplyTerm()).isEqualTo(12);
            assertThat(result.getPurpose()).isEqualTo("装修");
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
            assertThat(sampleApplication.getSubmitDate()).isNotNull();
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

        @Test
        @DisplayName("申请不存在应抛出异常")
        void shouldThrowExceptionWhenNotFound() {
            given(applicationRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> applicationService.submitApplication(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("申请不存在");
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
            given(calculatorRegistry.getCalculator(RepaymentMethod.EQUAL_INSTALLMENT)).willReturn(repaymentCalculator);
            doAnswer(invocation -> {
                RepaymentPeriod period = invocation.getArgument(0);
                period.setTotalAmount(new BigDecimal("8888.88"));
                return null;
            }).when(repaymentCalculator).calculate(any(RepaymentPeriod.class), any(BigDecimal.class),
                    any(BigDecimal.class), any(Integer.class), any(Integer.class), any(BigDecimal.class));

            applicationService.reviewApplication(1L, ApplicationStatus.APPROVED, "审核员",
                    "信用良好", new BigDecimal("100000"), 12, new BigDecimal("0.12"));

            assertThat(sampleApplication.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
            assertThat(sampleApplication.getApprovedAmount()).isEqualByComparingTo(new BigDecimal("100000"));
            assertThat(sampleApplication.getApprovedTerm()).isEqualTo(12);
            assertThat(sampleApplication.getInterestRate()).isEqualByComparingTo(new BigDecimal("0.12"));
            assertThat(sampleApplication.getMonthlyPayment()).isEqualByComparingTo(new BigDecimal("8888.88"));
            assertThat(sampleApplication.getReviewer()).isEqualTo("审核员");
            assertThat(sampleApplication.getReviewComments()).isEqualTo("信用良好");
            assertThat(sampleApplication.getReviewDate()).isNotNull();
            verify(eventBus, times(1)).publish(any(ApplicationApprovedEvent.class));
        }

        @Test
        @DisplayName("审批通过应发布正确的事件内容")
        void shouldPublishCorrectEventOnApproval() {
            sampleApplication.setStatus(ApplicationStatus.PENDING);
            given(applicationRepository.findById(1L)).willReturn(Optional.of(sampleApplication));
            given(applicationRepository.save(any(LoanApplication.class))).willAnswer(invocation -> invocation.getArgument(0));
            given(calculatorRegistry.getCalculator(RepaymentMethod.EQUAL_INSTALLMENT)).willReturn(repaymentCalculator);
            doAnswer(invocation -> {
                RepaymentPeriod period = invocation.getArgument(0);
                period.setTotalAmount(new BigDecimal("8888.88"));
                return null;
            }).when(repaymentCalculator).calculate(any(RepaymentPeriod.class), any(BigDecimal.class),
                    any(BigDecimal.class), any(Integer.class), any(Integer.class), any(BigDecimal.class));

            applicationService.reviewApplication(1L, ApplicationStatus.APPROVED, "审核员",
                    "信用良好", new BigDecimal("100000"), 12, new BigDecimal("0.12"));

            verify(eventBus).publish(eventCaptor.capture());
            ApplicationApprovedEvent event = eventCaptor.getValue();
            assertThat(event.getApplicationId()).isEqualTo(1L);
            assertThat(event.getCustomerId()).isEqualTo(1L);
            assertThat(event.getProductId()).isEqualTo(1L);
            assertThat(event.getApprovedAmount()).isEqualByComparingTo(new BigDecimal("100000"));
            assertThat(event.getApprovedTerm()).isEqualTo(12);
            assertThat(event.getInterestRate()).isEqualByComparingTo(new BigDecimal("0.12"));
            assertThat(event.getReviewer()).isEqualTo("审核员");
            assertThat(event.getReviewComments()).isEqualTo("信用良好");
        }

        @Test
        @DisplayName("驳回应更新状态且不发布事件")
        void shouldRejectSuccessfully() {
            sampleApplication.setStatus(ApplicationStatus.PENDING);
            given(applicationRepository.findById(1L)).willReturn(Optional.of(sampleApplication));
            given(applicationRepository.save(any(LoanApplication.class))).willAnswer(invocation -> invocation.getArgument(0));

            applicationService.reviewApplication(1L, ApplicationStatus.REJECTED, "审核员",
                    "资料不足", null, null, null);

            assertThat(sampleApplication.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
            assertThat(sampleApplication.getReviewer()).isEqualTo("审核员");
            assertThat(sampleApplication.getReviewComments()).isEqualTo("资料不足");
            assertThat(sampleApplication.getReviewDate()).isNotNull();
            verify(eventBus, never()).publish(any());
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

        @Test
        @DisplayName("申请不存在应抛出异常")
        void shouldThrowExceptionWhenNotFound() {
            given(applicationRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> applicationService.reviewApplication(99L, ApplicationStatus.APPROVED,
                    "审核员", "ok", null, null, null))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("申请不存在");
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

        @Test
        @DisplayName("申请不存在应抛出异常")
        void shouldThrowExceptionWhenNotFound() {
            given(applicationRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> applicationService.cancelApplication(99L, "test"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("申请不存在");
        }

        @Test
        @DisplayName("草稿状态的申请可以取消")
        void shouldCancelDraftSuccessfully() {
            given(applicationRepository.findById(1L)).willReturn(Optional.of(sampleApplication));
            given(applicationRepository.save(any(LoanApplication.class))).willAnswer(invocation -> invocation.getArgument(0));

            applicationService.cancelApplication(1L, "取消草稿");

            assertThat(sampleApplication.getStatus()).isEqualTo(ApplicationStatus.CANCELLED);
        }

        @Test
        @DisplayName("已拒绝的申请可以取消")
        void shouldCancelRejectedSuccessfully() {
            sampleApplication.setStatus(ApplicationStatus.REJECTED);
            given(applicationRepository.findById(1L)).willReturn(Optional.of(sampleApplication));
            given(applicationRepository.save(any(LoanApplication.class))).willAnswer(invocation -> invocation.getArgument(0));

            applicationService.cancelApplication(1L, "重新申请");

            assertThat(sampleApplication.getStatus()).isEqualTo(ApplicationStatus.CANCELLED);
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
            assertThat(result.get().getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("按ID查询不存在的申请应返回空")
        void shouldReturnEmptyWhenNotFound() {
            given(applicationRepository.findById(99L)).willReturn(Optional.empty());

            Optional<LoanApplication> result = applicationService.getApplicationById(99L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("按客户ID分页查询应返回结果")
        void shouldReturnByCustomerId() {
            Page<LoanApplication> page = new PageImpl<>(Collections.singletonList(sampleApplication));
            given(applicationRepository.findByCustomerId(eq(1L), any(PageRequest.class))).willReturn(page);

            Page<LoanApplication> result = applicationService.getApplicationsByCustomerId(1L, 0, 10);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("按客户ID分页查询无结果应返回空页")
        void shouldReturnEmptyPageByCustomerId() {
            Page<LoanApplication> emptyPage = new PageImpl<>(Collections.emptyList());
            given(applicationRepository.findByCustomerId(eq(2L), any(PageRequest.class))).willReturn(emptyPage);

            Page<LoanApplication> result = applicationService.getApplicationsByCustomerId(2L, 0, 10);

            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("条件查询 getApplicationList")
    class GetApplicationList {

        @Test
        @DisplayName("无条件查询应返回所有申请")
        void shouldReturnAllWhenNoFilters() {
            Page<LoanApplication> page = new PageImpl<>(List.of(sampleApplication));
            given(applicationRepository.findAll(any(Specification.class), any(PageRequest.class))).willReturn(page);

            Page<LoanApplication> result = applicationService.getApplicationList(null, null, null, 0, 10);

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("按客户ID过滤应返回对应申请")
        void shouldFilterByCustomerId() {
            Page<LoanApplication> page = new PageImpl<>(List.of(sampleApplication));
            given(applicationRepository.findAll(any(Specification.class), any(PageRequest.class))).willReturn(page);

            Page<LoanApplication> result = applicationService.getApplicationList(1L, null, null, 0, 10);

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("按产品ID过滤应返回对应申请")
        void shouldFilterByProductId() {
            Page<LoanApplication> page = new PageImpl<>(List.of(sampleApplication));
            given(applicationRepository.findAll(any(Specification.class), any(PageRequest.class))).willReturn(page);

            Page<LoanApplication> result = applicationService.getApplicationList(null, 1L, null, 0, 10);

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("按状态过滤应返回对应申请")
        void shouldFilterByStatus() {
            Page<LoanApplication> page = new PageImpl<>(List.of(sampleApplication));
            given(applicationRepository.findAll(any(Specification.class), any(PageRequest.class))).willReturn(page);

            Page<LoanApplication> result = applicationService.getApplicationList(null, null, ApplicationStatus.DRAFT, 0, 10);

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("多条件组合过滤应返回对应申请")
        void shouldFilterByMultipleCriteria() {
            Page<LoanApplication> page = new PageImpl<>(List.of(sampleApplication));
            given(applicationRepository.findAll(any(Specification.class), any(PageRequest.class))).willReturn(page);

            Page<LoanApplication> result = applicationService.getApplicationList(
                    1L, 1L, ApplicationStatus.DRAFT, 0, 10);

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("无匹配结果应返回空页")
        void shouldReturnEmptyWhenNoMatch() {
            Page<LoanApplication> emptyPage = new PageImpl<>(Collections.emptyList());
            given(applicationRepository.findAll(any(Specification.class), any(PageRequest.class))).willReturn(emptyPage);

            Page<LoanApplication> result = applicationService.getApplicationList(99L, null, null, 0, 10);

            assertThat(result.getContent()).isEmpty();
        }
    }
}
