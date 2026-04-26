package com.credit.system.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.credit.system.domain.LoanContract;
import com.credit.system.domain.enums.ContractStatus;
import com.credit.system.dto.ApiResponse;
import com.credit.system.dto.ContractRequest;
import com.credit.system.dto.ContractResponse;
import com.credit.system.exception.ResourceNotFoundException;
import com.credit.system.service.ContractService;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContractController 单元测试")
class ContractControllerTest {

    @Mock
    private ContractService contractService;

    @InjectMocks
    private ContractController contractController;

    private LoanContract sampleContract;

    @BeforeEach
    void setUp() {
        sampleContract = new LoanContract();
        sampleContract.setId(1L);
        sampleContract.setContractNo("LOAN20260426001");
        sampleContract.setCustomerId(1L);
        sampleContract.setApplicationId(1L);
        sampleContract.setTotalAmount(new BigDecimal("100000"));
        sampleContract.setInterestRate(new BigDecimal("0.12"));
        sampleContract.setTerm(12);
        sampleContract.setStartDate(LocalDate.now());
        sampleContract.setEndDate(LocalDate.now().plusMonths(12));
        sampleContract.setStatus(ContractStatus.ACTIVE);
    }

    @Nested
    @DisplayName("POST /api/contracts")
    class CreateContract {

        @Test
        @DisplayName("应成功创建并返回200")
        void shouldCreateSuccessfully() {
            given(contractService.createContract(any(LoanContract.class), anyString()))
                    .willReturn(sampleContract);

            ContractRequest request = new ContractRequest();

            ResponseEntity<ApiResponse<ContractResponse>> response =
                    contractController.createContract(request, "SYSTEM");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getCode()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("GET /api/contracts/{id}")
    class GetContract {

        @Test
        @DisplayName("应返回合同信息")
        void shouldReturnContract() {
            given(contractService.getContractById(1L)).willReturn(Optional.of(sampleContract));

            ResponseEntity<ApiResponse<ContractResponse>> response =
                    contractController.getContract(1L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData().getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("不存在的合同应抛出异常")
        void shouldThrowExceptionWhenNotFound() {
            given(contractService.getContractById(999L)).willReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> contractController.getContract(999L));
        }
    }

    @Nested
    @DisplayName("GET /api/contracts/no/{contractNo}")
    class GetByNo {

        @Test
        @DisplayName("应返回合同信息")
        void shouldReturnContract() {
            given(contractService.getContractByNo("LOAN20260426001"))
                    .willReturn(Optional.of(sampleContract));

            ResponseEntity<ApiResponse<ContractResponse>> response =
                    contractController.getByNo("LOAN20260426001");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("不存在的合同号应返回404")
        void shouldReturn404WhenNotFound() {
            given(contractService.getContractByNo("INVALID")).willReturn(Optional.empty());

            ResponseEntity<ApiResponse<ContractResponse>> response =
                    contractController.getByNo("INVALID");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("GET /api/contracts/by-application/{applicationId}")
    class GetByApplication {

        @Test
        @DisplayName("应返回合同信息")
        void shouldReturnContract() {
            given(contractService.getContractByApplicationId(1L))
                    .willReturn(Optional.of(sampleContract));

            ResponseEntity<ApiResponse<ContractResponse>> response =
                    contractController.getByApplication(1L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("不存在的关联应返回404")
        void shouldReturn404WhenNotFound() {
            given(contractService.getContractByApplicationId(999L))
                    .willReturn(Optional.empty());

            ResponseEntity<ApiResponse<ContractResponse>> response =
                    contractController.getByApplication(999L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("GET /api/contracts/customer/{customerId}")
    class GetByCustomer {

        @Test
        @DisplayName("应返回客户合同列表")
        void shouldReturnContracts() {
            given(contractService.getContractsByCustomerId(1L))
                    .willReturn(Collections.singletonList(sampleContract));

            ResponseEntity<ApiResponse<List<ContractResponse>>> response =
                    contractController.getByCustomer(1L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("GET /api/contracts （分页）")
    class ListContracts {

        @Test
        @DisplayName("应返回分页结果")
        void shouldReturnPagedResult() {
            Page<LoanContract> page = new PageImpl<>(Collections.singletonList(sampleContract));
            given(contractService.getContractList(any(), any(), any(), anyInt(), anyInt()))
                    .willReturn(page);

            ResponseEntity<ApiResponse<Page<ContractResponse>>> response =
                    contractController.listContracts(null, null, null, 0, 10);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData().getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("POST /api/contracts/{id}/sign")
    class SignContract {

        @Test
        @DisplayName("应成功签署并返回200")
        void shouldSignSuccessfully() {
            doNothing().when(contractService).signContract(1L, "张三", "ONLINE");

            ResponseEntity<ApiResponse<String>> response =
                    contractController.signContract(1L, "张三", "ONLINE");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getCode()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("POST /api/contracts/{id}/terminate")
    class TerminateContract {

        @Test
        @DisplayName("应成功终止并返回200")
        void shouldTerminateSuccessfully() {
            doNothing().when(contractService).terminateContract(1L, "违约", "ADMIN");

            ResponseEntity<ApiResponse<String>> response =
                    contractController.terminateContract(1L, "违约", "ADMIN");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Nested
    @DisplayName("POST /api/contracts/{id}/extend")
    class ExtendContract {

        @Test
        @DisplayName("应成功展期并返回200")
        void shouldExtendSuccessfully() {
            doNothing().when(contractService).extendContract(1L, 3, "资金周转困难", "ADMIN");

            ResponseEntity<ApiResponse<String>> response =
                    contractController.extendContract(1L, 3, "资金周转困难", "ADMIN");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Nested
    @DisplayName("POST /api/contracts/{id}/settle")
    class SettleContract {

        @Test
        @DisplayName("应成功结清并返回200")
        void shouldSettleSuccessfully() {
            doNothing().when(contractService).settleContract(1L);

            ResponseEntity<ApiResponse<String>> response =
                    contractController.settleContract(1L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Nested
    @DisplayName("GET /api/contracts/overdue")
    class GetOverdue {

        @Test
        @DisplayName("应返回逾期合同列表")
        void shouldReturnOverdueContracts() {
            given(contractService.getOverdueContracts())
                    .willReturn(Collections.singletonList(sampleContract));

            ResponseEntity<ApiResponse<List<ContractResponse>>> response =
                    contractController.getOverdue();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("GET /api/contracts/due-between")
    class GetDueBetween {

        @Test
        @DisplayName("应返回到期合同列表")
        void shouldReturnContractsDueBetween() {
            given(contractService.getContractsDueBetween(any(LocalDate.class), any(LocalDate.class)))
                    .willReturn(Collections.singletonList(sampleContract));

            ResponseEntity<ApiResponse<List<ContractResponse>>> response =
                    contractController.getDueBetween(LocalDate.now(), LocalDate.now().plusDays(30));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData()).hasSize(1);
        }
    }
}
