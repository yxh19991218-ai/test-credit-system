package com.credit.system;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 贷款申请完整流程集成测试。
 * <p>
 * 测试场景：创建客户 → 创建产品 → 发布产品 → 创建申请 → 提交申请 → 审核通过
 * </p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("贷款申请完整流程集成测试")
class LoanApplicationFlowIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String baseUrl;
    private String adminToken;

    // 测试数据
    private Long customerId;
    private Long productId;
    private Long applicationId;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/credit-system";
        // 获取管理员 token
        adminToken = login("admin", "admin123");
        System.out.println(">>> 登录成功, token: " + adminToken.substring(0, 20) + "...");
    }

    // ==================== 辅助方法 ====================

    @SuppressWarnings("unchecked")
    private String login(String username, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl + "/api/auth/login", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data).isNotNull();
        return (String) data.get("accessToken");
    }

    /** 创建带认证头的请求实体 */
    private <T> HttpEntity<T> authRequest(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);
        return new HttpEntity<>(body, headers);
    }

    /** 创建不带 body 的认证请求 */
    private HttpEntity<Void> authGet() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        return new HttpEntity<>(headers);
    }

    /** 检查响应是否成功（code == 200） */
    @SuppressWarnings("unchecked")
    private void assertSuccess(ResponseEntity<Map> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat((int) response.getBody().get("code")).isEqualTo(200);
    }

    // ==================== 测试步骤 ====================

    @SuppressWarnings("unchecked")
    @Test
    @Order(1)
    @DisplayName("步骤1: 创建客户")
    void step1_createCustomer() {
        Map<String, Object> request = new HashMap<>();
        request.put("name", "张三");
        request.put("idCard", "110101199001011234");
        request.put("phone", "13800138000");
        request.put("email", "zhangsan@example.com");
        request.put("occupation", "工程师");
        request.put("monthlyIncome", 15000);
        request.put("address", "北京市朝阳区建国路88号");
        request.put("creditScore", 750);
        request.put("bankCardNo", "6222021234567890");
        request.put("bankName", "中国工商银行");
        request.put("emergencyContactName", "李四");
        request.put("emergencyContactPhone", "13900139000");

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/customers",
                HttpMethod.POST,
                authRequest(request),
                Map.class);

        assertSuccess(response);

        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data).isNotNull();
        assertThat(data.get("id")).isNotNull();
        assertThat(data.get("name")).isEqualTo("张三");

        // Convert id to Long
        Object idObj = data.get("id");
        if (idObj instanceof Integer) {
            customerId = ((Integer) idObj).longValue();
        } else {
            customerId = (Long) idObj;
        }

        System.out.println(">>> 客户创建成功, ID: " + customerId);
    }

    @SuppressWarnings("unchecked")
    @Test
    @Order(2)
    @DisplayName("步骤2: 创建贷款产品")
    void step2_createProduct() {
        Map<String, Object> request = new HashMap<>();
        request.put("productCode", "LOAN_TEST_001");
        request.put("productName", "测试贷款产品");
        request.put("productDescription", "用于集成测试的贷款产品");
        request.put("interestRate", 0.12);
        request.put("repaymentMethod", "EQUAL_INSTALLMENT");
        request.put("minAmount", 10000);
        request.put("maxAmount", 500000);
        request.put("minTerm", 3);
        request.put("maxTerm", 36);
        request.put("minAge", 18);
        request.put("maxAge", 65);
        request.put("minCreditScore", 600);
        request.put("minMonthlyIncome", 5000);
        request.put("validFrom", LocalDate.now().minusDays(1).toString());
        request.put("validTo", LocalDate.now().plusYears(1).toString());

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/products",
                HttpMethod.POST,
                authRequest(request),
                Map.class);

        assertSuccess(response);

        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data).isNotNull();
        assertThat(data.get("id")).isNotNull();
        assertThat(data.get("productCode")).isEqualTo("LOAN_TEST_001");
        assertThat(data.get("status")).isEqualTo("DRAFT");

        Object idObj = data.get("id");
        if (idObj instanceof Integer) {
            productId = ((Integer) idObj).longValue();
        } else {
            productId = (Long) idObj;
        }

        System.out.println(">>> 产品创建成功, ID: " + productId + ", 状态: " + data.get("status"));
    }

    @Test
    @Order(3)
    @DisplayName("步骤3: 发布产品")
    void step3_publishProduct() {
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/products/" + productId + "/publish?operator=ADMIN",
                HttpMethod.POST,
                authGet(),
                Map.class);

        assertSuccess(response);
        String data = (String) response.getBody().get("data");
        assertThat(data).contains("产品发布成功");

        System.out.println(">>> 产品发布成功, ID: " + productId);
    }

    @SuppressWarnings("unchecked")
    @Test
    @Order(4)
    @DisplayName("步骤4: 创建贷款申请（草稿）")
    void step4_createApplication() {
        Map<String, Object> request = new HashMap<>();
        request.put("customerId", customerId.intValue());
        request.put("productId", productId.intValue());
        request.put("applyAmount", 100000);
        request.put("applyTerm", 12);
        request.put("purpose", "房屋装修");

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/applications?operator=张三",
                HttpMethod.POST,
                authRequest(request),
                Map.class);

        assertSuccess(response);

        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data).isNotNull();
        assertThat(data.get("id")).isNotNull();
        assertThat(data.get("customerId")).isNotNull();
        assertThat(data.get("productId")).isNotNull();
        assertThat(data.get("applyAmount")).isNotNull();
        assertThat(data.get("applyTerm")).isEqualTo(12);
        assertThat(data.get("purpose")).isEqualTo("房屋装修");
        assertThat(data.get("status")).isEqualTo("DRAFT");

        Object idObj = data.get("id");
        if (idObj instanceof Integer) {
            applicationId = ((Integer) idObj).longValue();
        } else {
            applicationId = (Long) idObj;
        }

        System.out.println(">>> 贷款申请创建成功, ID: " + applicationId + ", 状态: " + data.get("status"));
    }

    @Test
    @Order(5)
    @DisplayName("步骤5: 提交申请（草稿→待审批）")
    void step5_submitApplication() {
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/applications/" + applicationId + "/submit",
                HttpMethod.POST,
                authGet(),
                Map.class);

        assertSuccess(response);
        String data = (String) response.getBody().get("data");
        assertThat(data).contains("申请已提交");

        System.out.println(">>> 申请提交成功, ID: " + applicationId);
    }

    @Test
    @Order(6)
    @DisplayName("步骤6: 审核通过申请")
    void step6_approveApplication() {
        Map<String, Object> reviewRequest = new HashMap<>();
        reviewRequest.put("decision", "APPROVED");
        reviewRequest.put("reviewer", "审核员");
        reviewRequest.put("comments", "审核通过，符合贷款条件");
        reviewRequest.put("approvedAmount", 100000);
        reviewRequest.put("approvedTerm", 12);
        reviewRequest.put("interestRate", 0.12);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/applications/" + applicationId + "/review",
                HttpMethod.POST,
                authRequest(reviewRequest),
                Map.class);

        assertSuccess(response);
        String data = (String) response.getBody().get("data");
        assertThat(data).contains("审核完成");

        System.out.println(">>> 申请审核通过, ID: " + applicationId);
    }

    @SuppressWarnings("unchecked")
    @Test
    @Order(7)
    @DisplayName("步骤7: 查询申请详情，验证完整状态")
    void step7_verifyApplicationDetails() {
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/applications/" + applicationId,
                HttpMethod.GET,
                authGet(),
                Map.class);

        assertSuccess(response);

        Map<String, Object> app = (Map<String, Object>) response.getBody().get("data");
        assertThat(app).isNotNull();

        // 验证最终状态
        assertThat(app.get("status")).isEqualTo("APPROVED");
        assertThat(app.get("approvedAmount")).isNotNull();
        assertThat(app.get("approvedTerm")).isEqualTo(12);
        assertThat(app.get("interestRate")).isNotNull();
        assertThat(app.get("reviewer")).isEqualTo("审核员");
        assertThat(app.get("reviewComments")).isEqualTo("审核通过，符合贷款条件");

        // 验证月供已计算（等额本息）
        assertThat(app.get("monthlyPayment")).isNotNull();
        Object monthlyPayment = app.get("monthlyPayment");
        if (monthlyPayment instanceof Number) {
            assertThat(((Number) monthlyPayment).doubleValue()).isGreaterThan(0);
        }

        System.out.println(">>> 申请详情验证通过");
        System.out.println("    状态: " + app.get("status"));
        System.out.println("    审批金额: " + app.get("approvedAmount"));
        System.out.println("    审批期限: " + app.get("approvedTerm") + "个月");
        System.out.println("    年利率: " + app.get("interestRate"));
        System.out.println("    月供: " + app.get("monthlyPayment"));
        System.out.println("    审核人: " + app.get("reviewer"));
        System.out.println("    审核意见: " + app.get("reviewComments"));
    }

    @SuppressWarnings("unchecked")
    @Test
    @Order(8)
    @DisplayName("步骤8: 查询申请列表，验证包含新创建的申请")
    void step8_listApplications() {
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/applications?customerId=" + customerId + "&page=0&size=10",
                HttpMethod.GET,
                authGet(),
                Map.class);

        assertSuccess(response);

        Map<String, Object> page = (Map<String, Object>) response.getBody().get("data");
        assertThat(page).isNotNull();

        java.util.List<Map<String, Object>> content =
                (java.util.List<Map<String, Object>>) page.get("content");
        assertThat(content).isNotEmpty();

        boolean found = content.stream()
                .anyMatch(a -> {
                    Object idObj = a.get("id");
                    Long id;
                    if (idObj instanceof Integer) {
                        id = ((Integer) idObj).longValue();
                    } else {
                        id = (Long) idObj;
                    }
                    return id.equals(applicationId);
                });
        assertThat(found).isTrue();

        System.out.println(">>> 申请列表查询成功, 总数: " + page.get("totalElements"));
    }
}
