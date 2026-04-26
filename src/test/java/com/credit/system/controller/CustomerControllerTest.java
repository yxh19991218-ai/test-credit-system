package com.credit.system.controller;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.credit.system.domain.Customer;
import com.credit.system.domain.enums.CustomerStatus;
import com.credit.system.dto.ApiResponse;
import com.credit.system.dto.CustomerRequest;
import com.credit.system.dto.CustomerResponse;
import com.credit.system.dto.CustomerStatusRequest;
import com.credit.system.dto.PageRequestDTO;
import com.credit.system.exception.BusinessException;
import com.credit.system.exception.ResourceNotFoundException;
import com.credit.system.service.CustomerService;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerController 单元测试")
class CustomerControllerTest {

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private CustomerController customerController;

    private Customer sampleCustomer;
    private CustomerRequest customerRequest;

    @BeforeEach
    void setUp() {
        sampleCustomer = new Customer();
        sampleCustomer.setId(1L);
        sampleCustomer.setName("张三");
        sampleCustomer.setIdCard("110101199001011234");
        sampleCustomer.setPhone("13800138000");
        sampleCustomer.setStatus(CustomerStatus.NORMAL);

        customerRequest = new CustomerRequest();
        customerRequest.setName("张三");
        customerRequest.setIdCard("110101199001011234");
        customerRequest.setPhone("13800138000");
    }

    @Nested
    @DisplayName("POST /api/customers")
    class CreateCustomer {

        @Test
        @DisplayName("应成功创建并返回200")
        void shouldCreateSuccessfully() {
            given(customerService.createCustomer(any(Customer.class), isNull()))
                    .willReturn(sampleCustomer);

            ResponseEntity<ApiResponse<CustomerResponse>> response =
                    customerController.createCustomer(customerRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getCode()).isEqualTo(200);
            assertThat(response.getBody().getData().getName()).isEqualTo("张三");
        }
    }

    @Nested
    @DisplayName("GET /api/customers/{id}")
    class GetCustomer {

        @Test
        @DisplayName("应返回客户信息")
        void shouldReturnCustomer() {
            given(customerService.getCustomerById(1L)).willReturn(Optional.of(sampleCustomer));

            ResponseEntity<ApiResponse<CustomerResponse>> response =
                    customerController.getCustomerById(1L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData().getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("不存在的客户应返回404")
        void shouldReturn404WhenNotFound() {
            given(customerService.getCustomerById(999L)).willReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> customerController.getCustomerById(999L));
        }
    }

    @Nested
    @DisplayName("GET /api/customers （分页）")
    class ListCustomers {

        @Test
        @DisplayName("应返回分页结果")
        void shouldReturnPagedResult() {
            Page<Customer> page = new PageImpl<>(Collections.singletonList(sampleCustomer));
            given(customerService.getCustomerList(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), anyInt(), anyInt()))
                    .willReturn(page);

            PageRequestDTO dto = new PageRequestDTO();
            ResponseEntity<ApiResponse<Page<CustomerResponse>>> response =
                    customerController.listCustomers(dto);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData().getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("PATCH /api/customers/{id}/status")
    class UpdateCustomerStatus {

        @Test
        @DisplayName("应成功更新并返回200")
        void shouldReturn200() {
            lenient().doNothing().when(customerService).updateCustomerStatus(eq(1L), eq(CustomerStatus.FROZEN), anyString(), anyString());

            CustomerStatusRequest request = new CustomerStatusRequest();
            request.setStatus("FROZEN");
            request.setReason("测试");

            ResponseEntity<ApiResponse<String>> response =
                    customerController.updateCustomerStatus(1L, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getCode()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("DELETE /api/customers/{id}")
    class DeleteCustomer {

        @Test
        @DisplayName("应成功删除返回200")
        void shouldReturn200() {
            lenient().doNothing().when(customerService).deleteCustomer(eq(1L), anyString(), anyString());

            ResponseEntity<ApiResponse<String>> response =
                    customerController.deleteCustomer(1L, "删除测试", "ADMIN");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Nested
    @DisplayName("全局异常处理")
    class ExceptionHandler {

        @Test
        @DisplayName("ResourceNotFoundException 应返回404")
        void shouldReturn404ForNotFound() {
            GlobalExceptionHandler handler = new GlobalExceptionHandler();

            ResponseEntity<ApiResponse<Void>> response =
                    handler.handleNotFound(new ResourceNotFoundException("未找到"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("BusinessException 应返回400")
        void shouldReturn400ForBusiness() {
            GlobalExceptionHandler handler = new GlobalExceptionHandler();

            ResponseEntity<ApiResponse<Void>> response =
                    handler.handleBusiness(new BusinessException("业务错误"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    private void assertThatThrownBy(Runnable runnable, Class<? extends Throwable> exceptionClass) {
        try {
            runnable.run();
            throw new AssertionError("Expected exception but none was thrown");
        } catch (Throwable t) {
            if (!exceptionClass.isInstance(t)) {
                throw new AssertionError("Expected " + exceptionClass.getSimpleName() + " but got " + t.getClass().getSimpleName() + ": " + t.getMessage());
            }
        }
    }
}
