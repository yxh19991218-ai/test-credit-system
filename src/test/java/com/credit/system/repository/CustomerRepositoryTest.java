package com.credit.system.repository;

import com.credit.system.domain.Customer;
import com.credit.system.domain.enums.CustomerStatus;
import com.credit.system.domain.enums.RiskLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@EnableJpaAuditing
@DisplayName("CustomerRepository 集成测试")
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    @DisplayName("应通过身份证号查找到客户")
    void shouldFindCustomerByIdCard() {
        customerRepository.save(createTestCustomer("110101199001011236", "13800138002"));

        Optional<Customer> found = customerRepository.findByIdCard("110101199001011236");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("测试客户");
    }

    @Test
    @DisplayName("应检测身份证号是否存在")
    void shouldCheckIdCardExists() {
        customerRepository.save(createTestCustomer("110101199001011237", "13800138003"));

        assertThat(customerRepository.existsByIdCard("110101199001011237")).isTrue();
        assertThat(customerRepository.existsByIdCard("999999999999999999")).isFalse();
    }

    @Test
    @DisplayName("应通过手机号查找到客户")
    void shouldFindCustomerByPhone() {
        customerRepository.save(createTestCustomer("110101199001011238", "13800138004"));

        Optional<Customer> found = customerRepository.findByPhone("13800138004");
        assertThat(found).isPresent();
    }

    @Test
    @DisplayName("应通过状态查找到客户")
    void shouldFindCustomerByStatus() {
        customerRepository.save(createTestCustomer("110101199001011239", "13800138005"));
        Customer c2 = createTestCustomer("220101199001011240", "13900139001");
        c2.setStatus(CustomerStatus.FROZEN);
        customerRepository.save(c2);

        List<Customer> normals = customerRepository.findByStatus(CustomerStatus.NORMAL);
        List<Customer> frozens = customerRepository.findByStatus(CustomerStatus.FROZEN);

        assertThat(normals).hasSize(1);
        assertThat(frozens).hasSize(1);
    }

    @Test
    @DisplayName("黑名单检查应返回正确结果")
    void shouldCheckBlacklist() {
        Customer c = createTestCustomer("110101199001011241", "13800138006");
        c.setStatus(CustomerStatus.BLACKLIST);
        c.setStatusReason("恶意逾期");
        customerRepository.save(c);

        assertThat(customerRepository.existsInBlacklistByIdCard("110101199001011241")).isTrue();
        assertThat(customerRepository.existsInBlacklistByPhone("13800138006")).isTrue();
    }

    @Test
    @DisplayName("征信分数范围查询应返回正确结果")
    void shouldFindByCreditScoreRange() {
        Customer c1 = createTestCustomer("110101199001011242", "13800138007");
        c1.setCreditScore(650);
        Customer c2 = createTestCustomer("220101199001011235", "13900139000");
        c2.setCreditScore(750);
        customerRepository.save(c1);
        customerRepository.save(c2);

        List<Customer> results = customerRepository.findByCreditScoreBetween(700, 800);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCreditScore()).isEqualTo(750);
    }

    @Test
    @DisplayName("风险等级查询应返回正确结果")
    void shouldFindByRiskLevel() {
        Customer c = createTestCustomer("110101199001011243", "13800138008");
        c.setRiskLevel(RiskLevel.LOW);
        customerRepository.save(c);

        List<Customer> results = customerRepository.findByRiskLevel(RiskLevel.LOW);
        assertThat(results).hasSize(1);
    }

    private Customer createTestCustomer(String idCard, String phone) {
        Customer c = new Customer();
        c.setName("测试客户");
        c.setIdCard(idCard);
        c.setPhone(phone);
        c.setStatus(CustomerStatus.NORMAL);
        c.setMonthlyIncome(new BigDecimal("10000"));
        return c;
    }
}
