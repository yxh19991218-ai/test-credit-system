package com.credit.system.service.impl;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.credit.system.domain.Customer;
import com.credit.system.domain.enums.CustomerStatus;
import com.credit.system.exception.BusinessException;
import com.credit.system.exception.ResourceNotFoundException;
import com.credit.system.repository.CustomerDocumentRepository;
import com.credit.system.repository.CustomerRepository;
import com.credit.system.service.specification.CustomerSpecification;
import com.credit.system.service.specification.SpecificationResult;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerServiceImpl 单元测试")
class CustomerServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerDocumentRepository documentRepository;

    @Mock
    private CustomerSpecification<Customer> mockSpec1;

    @Mock
    private CustomerSpecification<Customer> mockSpec2;

    private CustomerServiceImpl customerService;

    private Customer sampleCustomer;

    @BeforeEach
    void setUp() {
        // 使用 mock 规约，不依赖具体规约实现
        List<CustomerSpecification<Customer>> specs = List.of(mockSpec1, mockSpec2);
        customerService = new CustomerServiceImpl(customerRepository, documentRepository, specs);

        sampleCustomer = new Customer();
        sampleCustomer.setId(1L);
        sampleCustomer.setName("张三");
        sampleCustomer.setIdCard("110101199001011234");
        sampleCustomer.setPhone("13800138000");
        sampleCustomer.setEmail("zhangsan@test.com");
        sampleCustomer.setOccupation("工程师");
        sampleCustomer.setMonthlyIncome(new BigDecimal("15000.00"));
        sampleCustomer.setAddress("北京市朝阳区");
        sampleCustomer.setStatus(CustomerStatus.NORMAL);
    }

    @Nested
    @DisplayName("创建客户 createCustomer")
    class CreateCustomer {

        @Test
        @DisplayName("应成功创建正常客户")
        void shouldCreateCustomerSuccessfully() {
            // given: 所有规约通过
            given(mockSpec1.isSatisfiedBy(any())).willReturn(SpecificationResult.SATISFIED);
            given(mockSpec2.isSatisfiedBy(any())).willReturn(SpecificationResult.SATISFIED);
            given(customerRepository.save(any(Customer.class))).willReturn(sampleCustomer);

            // when
            Customer result = customerService.createCustomer(sampleCustomer, null);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("张三");
            assertThat(result.getStatus()).isEqualTo(CustomerStatus.NORMAL);
            verify(customerRepository, times(1)).save(any(Customer.class));
        }

        @Test
        @DisplayName("规约验证失败应抛出异常")
        void shouldThrowExceptionWhenSpecificationFails() {
            // given: 第一个规约失败
            given(mockSpec1.isSatisfiedBy(any()))
                    .willReturn(SpecificationResult.unsatisfied("验证失败"));

            // when & then
            assertThatThrownBy(() -> customerService.createCustomer(sampleCustomer, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("验证失败");

            verify(customerRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("查询客户")
    class GetCustomer {

        @Test
        @DisplayName("按ID查询应返回客户")
        void shouldReturnCustomerById() {
            given(customerRepository.findById(1L)).willReturn(Optional.of(sampleCustomer));

            Optional<Customer> result = customerService.getCustomerById(1L);

            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("张三");
        }

        @Test
        @DisplayName("查询不存在的ID应返回空")
        void shouldReturnEmptyWhenNotFound() {
            given(customerRepository.findById(999L)).willReturn(Optional.empty());

            Optional<Customer> result = customerService.getCustomerById(999L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("按身份证查询应返回客户")
        void shouldFindCustomerByIdCard() {
            given(customerRepository.findByIdCard("110101199001011234")).willReturn(Optional.of(sampleCustomer));

            Optional<Customer> result = customerService.getCustomerByIdCard("110101199001011234");

            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("按手机号查询应返回客户")
        void shouldFindCustomerByPhone() {
            given(customerRepository.findByPhone("13800138000")).willReturn(Optional.of(sampleCustomer));

            Optional<Customer> result = customerService.getCustomerByPhone("13800138000");

            assertThat(result).isPresent();
        }
    }

    @Nested
    @DisplayName("更新客户 updateCustomer")
    class UpdateCustomer {

        @Test
        @DisplayName("应成功更新客户信息")
        void shouldUpdateCustomerSuccessfully() {
            Customer updated = new Customer();
            updated.setName("张三(改)");
            updated.setPhone("13900139000");
            updated.setEmail("new@test.com");

            given(customerRepository.findById(1L)).willReturn(Optional.of(sampleCustomer));
            given(customerRepository.existsByPhone("13900139000")).willReturn(false);
            given(customerRepository.save(any(Customer.class))).willAnswer(invocation -> invocation.getArgument(0));

            Customer result = customerService.updateCustomer(1L, updated, "OPERATOR");

            assertThat(result.getName()).isEqualTo("张三(改)");
            assertThat(result.getPhone()).isEqualTo("13900139000");
            assertThat(result.getEmail()).isEqualTo("new@test.com");
        }

        @Test
        @DisplayName("更新不存在的客户应抛出异常")
        void shouldThrowExceptionWhenNotFound() {
            given(customerRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> customerService.updateCustomer(999L, new Customer(), "OP"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("客户不存在");
        }

        @Test
        @DisplayName("冻结客户不能被修改")
        void shouldNotUpdateFrozenCustomer() {
            sampleCustomer.setStatus(CustomerStatus.FROZEN);
            given(customerRepository.findById(1L)).willReturn(Optional.of(sampleCustomer));

            assertThatThrownBy(() -> customerService.updateCustomer(1L, new Customer(), "OP"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("已被冻结");
        }
    }

    @Nested
    @DisplayName("更新客户状态 updateCustomerStatus")
    class UpdateCustomerStatus {

        @Test
        @DisplayName("应成功更新客户状态")
        void shouldUpdateStatusSuccessfully() {
            given(customerRepository.findById(1L)).willReturn(Optional.of(sampleCustomer));
            given(customerRepository.save(any(Customer.class))).willAnswer(invocation -> invocation.getArgument(0));

            customerService.updateCustomerStatus(1L, CustomerStatus.FROZEN, "测试冻结", "ADMIN");

            assertThat(sampleCustomer.getStatus()).isEqualTo(CustomerStatus.FROZEN);
        }

        @Test
        @DisplayName("已删除客户不能修改状态")
        void shouldNotChangeDeletedCustomerStatus() {
            sampleCustomer.setStatus(CustomerStatus.DELETED);
            given(customerRepository.findById(1L)).willReturn(Optional.of(sampleCustomer));

            assertThatThrownBy(() -> customerService.updateCustomerStatus(1L, CustomerStatus.NORMAL, "恢复", "ADMIN"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("已删除");
        }
    }

    @Nested
    @DisplayName("分页查询 getCustomerList")
    class GetCustomerList {

        @Test
        @DisplayName("应返回分页结果")
        void shouldReturnPagedResults() {
            Page<Customer> page = new PageImpl<>(Collections.singletonList(sampleCustomer));
            given(customerRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class))).willReturn(page);

            Page<Customer> result = customerService.getCustomerList(null, null, null, null, null, null, 0, 10);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("张三");
        }
    }
}
