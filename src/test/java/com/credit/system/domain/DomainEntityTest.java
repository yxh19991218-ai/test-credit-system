package com.credit.system.domain;

import com.credit.system.domain.enums.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Domain 实体单元测试")
class DomainEntityTest {

    @Test
    @DisplayName("Customer 实体应设置默认状态")
    void customerShouldHaveDefaultStatus() {
        Customer customer = new Customer();
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.NORMAL);
        assertThat(customer.getDocuments()).isEmpty();
    }

    @Test
    @DisplayName("LoanProduct 实体应设置默认状态")
    void loanProductShouldHaveDefaultStatus() {
        LoanProduct product = new LoanProduct();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.DRAFT);
        assertThat(product.getMinAge()).isEqualTo(18);
        assertThat(product.getMaxAge()).isEqualTo(65);
    }

    @Test
    @DisplayName("LoanApplication 实体应设置默认状态")
    void loanApplicationShouldHaveDefaultStatus() {
        LoanApplication app = new LoanApplication();
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.DRAFT);
    }

    @Test
    @DisplayName("LoanContract 实体应设置默认状态")
    void loanContractShouldHaveDefaultStatus() {
        LoanContract contract = new LoanContract();
        assertThat(contract.getStatus()).isEqualTo(ContractStatus.ACTIVE);
    }

    @Test
    @DisplayName("RepaymentSchedule 实体应设置默认状态")
    void repaymentScheduleShouldHaveDefaultStatus() {
        RepaymentSchedule schedule = new RepaymentSchedule();
        assertThat(schedule.getStatus()).isEqualTo(ScheduleStatus.ACTIVE);
    }

    @Test
    @DisplayName("RepaymentPeriod 实体应设置默认状态")
    void repaymentPeriodShouldHaveDefaultStatus() {
        RepaymentPeriod period = new RepaymentPeriod();
        assertThat(period.getStatus()).isEqualTo(RepaymentPeriodStatus.PENDING);
    }

    @Test
    @DisplayName("Customer 实体字段应有正确的setter/getter")
    void customerShouldHaveCorrectFields() {
        Customer customer = new Customer();
        customer.setName("测试用户");
        customer.setIdCard("110101199001011234");
        customer.setPhone("13800138000");
        customer.setEmail("test@test.com");
        customer.setOccupation("工程师");
        customer.setMonthlyIncome(new BigDecimal("15000.00"));

        assertThat(customer.getName()).isEqualTo("测试用户");
        assertThat(customer.getIdCard()).isEqualTo("110101199001011234");
        assertThat(customer.getPhone()).isEqualTo("13800138000");
        assertThat(customer.getEmail()).isEqualTo("test@test.com");
        assertThat(customer.getOccupation()).isEqualTo("工程师");
        assertThat(customer.getMonthlyIncome()).isEqualByComparingTo(new BigDecimal("15000.00"));
    }

    @Test
    @DisplayName("CustomerStatus 应包含所有状态")
    void customerStatusShouldContainAllValues() {
        assertThat(CustomerStatus.values()).containsExactly(
                CustomerStatus.NORMAL,
                CustomerStatus.FROZEN,
                CustomerStatus.BLACKLIST,
                CustomerStatus.DELETED
        );
    }

    @Test
    @DisplayName("RiskLevel 应包含所有等级")
    void riskLevelShouldContainAllValues() {
        assertThat(RiskLevel.values()).containsExactly(
                RiskLevel.LOW,
                RiskLevel.MEDIUM,
                RiskLevel.HIGH
        );
    }

    @Test
    @DisplayName("RepaymentMethod 应包含所有还款方式")
    void repaymentMethodShouldContainAllValues() {
        assertThat(RepaymentMethod.values()).containsExactly(
                RepaymentMethod.EQUAL_PRINCIPAL,
                RepaymentMethod.EQUAL_INSTALLMENT,
                RepaymentMethod.INTEREST_ONLY,
                RepaymentMethod.BALLOON,
                RepaymentMethod.DUE_ONE_TIME
        );
    }

    @Test
    @DisplayName("DocumentType 应包含所有类型")
    void documentTypeShouldContainAllValues() {
        assertThat(DocumentType.values()).containsExactly(
                DocumentType.ID_CARD,
                DocumentType.INCOME_PROOF,
                DocumentType.ADDRESS_PROOF,
                DocumentType.CREDIT_REPORT,
                DocumentType.CONTRACT,
                DocumentType.OTHER
        );
    }

    @Test
    @DisplayName("LoanProduct 应支持复杂属性")
    void loanProductShouldSupportComplexFields() {
        LoanProduct product = new LoanProduct();
        product.setProductCode("LOAN001");
        product.setProductName("消费贷");
        product.setInterestRate(new BigDecimal("0.12"));
        product.setMinAmount(new BigDecimal("5000"));
        product.setMaxAmount(new BigDecimal("200000"));
        product.setMinTerm(3);
        product.setMaxTerm(36);
        product.setRepaymentMethod(RepaymentMethod.EQUAL_INSTALLMENT);

        assertThat(product.getProductCode()).isEqualTo("LOAN001");
        assertThat(product.getRepaymentMethod()).isEqualTo(RepaymentMethod.EQUAL_INSTALLMENT);
    }
}
