package com.credit.system.service.specification.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.credit.system.domain.Customer;
import com.credit.system.repository.CustomerRepository;
import com.credit.system.service.specification.SpecificationResult;

@ExtendWith(MockitoExtension.class)
@DisplayName("客户规约实现 单元测试")
class CustomerSpecificationImplTest {

    @Mock
    private CustomerRepository customerRepository;

    @Nested
    @DisplayName("AgeSpecification 年龄验证")
    class AgeSpecificationTest {

        @Test
        @DisplayName("年龄在 18-65 之间应通过")
        void shouldPassForValidAge() {
            // 使用一个 30 岁左右的身份证号
            Customer customer = new Customer();
            customer.setIdCard("110101199001011234"); // 1990年出生，约36岁
            AgeSpecification spec = new AgeSpecification();

            SpecificationResult result = spec.isSatisfiedBy(customer);

            assertThat(result.satisfied()).isTrue();
        }

        @Test
        @DisplayName("年龄小于 18 岁应不通过")
        void shouldFailForUnderage() {
            Customer customer = new Customer();
            customer.setIdCard("110101201001011234"); // 2010年出生，约16岁
            AgeSpecification spec = new AgeSpecification();

            SpecificationResult result = spec.isSatisfiedBy(customer);

            assertThat(result.satisfied()).isFalse();
            assertThat(result.message()).contains("年龄");
        }

        @Test
        @DisplayName("年龄大于 65 岁应不通过")
        void shouldFailForOverAge() {
            Customer customer = new Customer();
            customer.setIdCard("110101195001011234"); // 1950年出生，约76岁
            AgeSpecification spec = new AgeSpecification();

            SpecificationResult result = spec.isSatisfiedBy(customer);

            assertThat(result.satisfied()).isFalse();
            assertThat(result.message()).contains("年龄");
        }

        @Test
        @DisplayName("自定义年龄范围应生效")
        void shouldUseCustomAgeRange() {
            Customer customer = new Customer();
            customer.setIdCard("110101199001011234"); // 1990年出生，约36岁
            AgeSpecification spec = new AgeSpecification(25, 30); // 25-30岁

            SpecificationResult result = spec.isSatisfiedBy(customer);

            assertThat(result.satisfied()).isFalse();
            assertThat(result.message()).contains("年龄");
        }
    }

    @Nested
    @DisplayName("BlacklistSpecification 黑名单检查")
    class BlacklistSpecificationTest {

        @Test
        @DisplayName("身份证和手机号均不在黑名单应通过")
        void shouldPassWhenNotInBlacklist() {
            Customer customer = new Customer();
            customer.setIdCard("110101199001011234");
            customer.setPhone("13800138000");

            given(customerRepository.existsInBlacklistByIdCard("110101199001011234")).willReturn(false);
            given(customerRepository.existsInBlacklistByPhone("13800138000")).willReturn(false);

            BlacklistSpecification spec = new BlacklistSpecification(customerRepository);
            SpecificationResult result = spec.isSatisfiedBy(customer);

            assertThat(result.satisfied()).isTrue();
        }

        @Test
        @DisplayName("身份证在黑名单应不通过")
        void shouldFailWhenIdCardInBlacklist() {
            Customer customer = new Customer();
            customer.setIdCard("110101199001011234");
            customer.setPhone("13800138000");

            given(customerRepository.existsInBlacklistByIdCard("110101199001011234")).willReturn(true);

            BlacklistSpecification spec = new BlacklistSpecification(customerRepository);
            SpecificationResult result = spec.isSatisfiedBy(customer);

            assertThat(result.satisfied()).isFalse();
            assertThat(result.message()).contains("黑名单");
        }

        @Test
        @DisplayName("手机号在黑名单应不通过")
        void shouldFailWhenPhoneInBlacklist() {
            Customer customer = new Customer();
            customer.setIdCard("110101199001011234");
            customer.setPhone("13800138000");

            given(customerRepository.existsInBlacklistByIdCard("110101199001011234")).willReturn(false);
            given(customerRepository.existsInBlacklistByPhone("13800138000")).willReturn(true);

            BlacklistSpecification spec = new BlacklistSpecification(customerRepository);
            SpecificationResult result = spec.isSatisfiedBy(customer);

            assertThat(result.satisfied()).isFalse();
            assertThat(result.message()).contains("黑名单");
        }
    }

    @Nested
    @DisplayName("IdCardUniquenessSpecification 身份证唯一性")
    class IdCardUniquenessSpecificationTest {

        @Test
        @DisplayName("身份证号不重复应通过")
        void shouldPassWhenIdCardUnique() {
            Customer customer = new Customer();
            customer.setIdCard("110101199001011234");

            given(customerRepository.existsByIdCard("110101199001011234")).willReturn(false);

            IdCardUniquenessSpecification spec = new IdCardUniquenessSpecification(customerRepository);
            SpecificationResult result = spec.isSatisfiedBy(customer);

            assertThat(result.satisfied()).isTrue();
        }

        @Test
        @DisplayName("身份证号重复应不通过")
        void shouldFailWhenIdCardDuplicated() {
            Customer customer = new Customer();
            customer.setIdCard("110101199001011234");

            given(customerRepository.existsByIdCard("110101199001011234")).willReturn(true);

            IdCardUniquenessSpecification spec = new IdCardUniquenessSpecification(customerRepository);
            SpecificationResult result = spec.isSatisfiedBy(customer);

            assertThat(result.satisfied()).isFalse();
            assertThat(result.message()).contains("身份证号已存在");
        }
    }

    @Nested
    @DisplayName("PhoneUniquenessSpecification 手机号唯一性")
    class PhoneUniquenessSpecificationTest {

        @Test
        @DisplayName("手机号不重复应通过")
        void shouldPassWhenPhoneUnique() {
            Customer customer = new Customer();
            customer.setPhone("13800138000");

            given(customerRepository.existsByPhone("13800138000")).willReturn(false);

            PhoneUniquenessSpecification spec = new PhoneUniquenessSpecification(customerRepository);
            SpecificationResult result = spec.isSatisfiedBy(customer);

            assertThat(result.satisfied()).isTrue();
        }

        @Test
        @DisplayName("手机号重复应不通过")
        void shouldFailWhenPhoneDuplicated() {
            Customer customer = new Customer();
            customer.setPhone("13800138000");

            given(customerRepository.existsByPhone("13800138000")).willReturn(true);

            PhoneUniquenessSpecification spec = new PhoneUniquenessSpecification(customerRepository);
            SpecificationResult result = spec.isSatisfiedBy(customer);

            assertThat(result.satisfied()).isFalse();
            assertThat(result.message()).contains("手机号已存在");
        }
    }

    @Nested
    @DisplayName("MonthlyIncomeSpecification 月收入验证")
    class MonthlyIncomeSpecificationTest {

        @Test
        @DisplayName("月收入为正数应通过")
        void shouldPassForPositiveIncome() {
            Customer customer = new Customer();
            customer.setMonthlyIncome(new BigDecimal("5000"));

            MonthlyIncomeSpecification spec = new MonthlyIncomeSpecification();
            SpecificationResult result = spec.isSatisfiedBy(customer);

            assertThat(result.satisfied()).isTrue();
        }

        @Test
        @DisplayName("月收入为零应通过")
        void shouldPassForZeroIncome() {
            Customer customer = new Customer();
            customer.setMonthlyIncome(BigDecimal.ZERO);

            MonthlyIncomeSpecification spec = new MonthlyIncomeSpecification();
            SpecificationResult result = spec.isSatisfiedBy(customer);

            assertThat(result.satisfied()).isTrue();
        }

        @Test
        @DisplayName("月收入为负数应不通过")
        void shouldFailForNegativeIncome() {
            Customer customer = new Customer();
            customer.setMonthlyIncome(new BigDecimal("-1000"));

            MonthlyIncomeSpecification spec = new MonthlyIncomeSpecification();
            SpecificationResult result = spec.isSatisfiedBy(customer);

            assertThat(result.satisfied()).isFalse();
            assertThat(result.message()).contains("月收入不能为负数");
        }

        @Test
        @DisplayName("月收入为 null 应通过")
        void shouldPassForNullIncome() {
            Customer customer = new Customer();
            customer.setMonthlyIncome(null);

            MonthlyIncomeSpecification spec = new MonthlyIncomeSpecification();
            SpecificationResult result = spec.isSatisfiedBy(customer);

            assertThat(result.satisfied()).isTrue();
        }
    }

    @Nested
    @DisplayName("EmergencyContactPhoneSpecification 紧急联系人电话验证")
    class EmergencyContactPhoneSpecificationTest {

        @Test
        @DisplayName("有效的手机号应通过")
        void shouldPassForValidPhone() {
            Customer customer = new Customer();
            customer.setEmergencyContactPhone("13800138000");

            EmergencyContactPhoneSpecification spec = new EmergencyContactPhoneSpecification();
            SpecificationResult result = spec.isSatisfiedBy(customer);

            assertThat(result.satisfied()).isTrue();
        }

        @Test
        @DisplayName("无效的手机号应不通过")
        void shouldFailForInvalidPhone() {
            Customer customer = new Customer();
            customer.setEmergencyContactPhone("12345");

            EmergencyContactPhoneSpecification spec = new EmergencyContactPhoneSpecification();
            SpecificationResult result = spec.isSatisfiedBy(customer);

            assertThat(result.satisfied()).isFalse();
            assertThat(result.message()).contains("手机号格式不正确");
        }

        @Test
        @DisplayName("紧急联系人电话为 null 应通过")
        void shouldPassForNullPhone() {
            Customer customer = new Customer();
            customer.setEmergencyContactPhone(null);

            EmergencyContactPhoneSpecification spec = new EmergencyContactPhoneSpecification();
            SpecificationResult result = spec.isSatisfiedBy(customer);

            assertThat(result.satisfied()).isTrue();
        }

        @Test
        @DisplayName("非 1 开头的手机号应不通过")
        void shouldFailForNonMobilePhone() {
            Customer customer = new Customer();
            customer.setEmergencyContactPhone("01012345678");

            EmergencyContactPhoneSpecification spec = new EmergencyContactPhoneSpecification();
            SpecificationResult result = spec.isSatisfiedBy(customer);

            assertThat(result.satisfied()).isFalse();
        }
    }
}
