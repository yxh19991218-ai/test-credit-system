package com.credit.system;

import org.springframework.boot.test.context.SpringBootTest;

import java.lang.annotation.*;

/**
 * 自定义测试注解：包含 Spring Boot 集成上下文，仅加载所需配置以加快测试速度
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public @interface IntegrationTest {
}
