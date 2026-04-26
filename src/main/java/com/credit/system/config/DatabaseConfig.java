package com.credit.system.config;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * 数据源配置 - 支持 Railway MySQL 和本地开发环境。
 * <p>
 * Railway MySQL 插件自动注入的环境变量（无下划线）：
 * - MYSQLHOST, MYSQLPORT, MYSQLUSER, MYSQLPASSWORD, MYSQLDATABASE, MYSQL_URL
 * 本地开发使用：DB_URL, DB_USERNAME, DB_PASSWORD
 */
@Configuration
public class DatabaseConfig {

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();

        // 1. 优先使用完整的 JDBC URL（Railway 的 MYSQL_URL 或本地的 DB_URL）
        String jdbcUrl = getEnv("MYSQL_URL", null);
        if (jdbcUrl == null) {
            jdbcUrl = getEnv("DB_URL", null);
        }

        if (jdbcUrl != null) {
            // 使用完整的 JDBC URL
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(getEnv("MYSQL_USER",
                    getEnv("DB_USERNAME", "root")));
            config.setPassword(getEnv("MYSQL_PASSWORD",
                    getEnv("DB_PASSWORD", "root123")));
        } else {
            // 2. 使用 Railway 的独立变量（MYSQLHOST, MYSQLPORT 等）
            String host = getEnv("MYSQLHOST", "localhost");
            String port = getEnv("MYSQLPORT", "3306");
            String database = getEnv("MYSQLDATABASE", "credit_system");
            String user = getEnv("MYSQLUSER",
                    getEnv("DB_USERNAME", "root"));
            String password = getEnv("MYSQLPASSWORD",
                    getEnv("DB_PASSWORD", "root123"));

            jdbcUrl = String.format(
                    "jdbc:mysql://%s:%s/%s?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&enabledTLSProtocols=TLSv1.2",
                    host, port, database);
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(user);
            config.setPassword(password);
        }

        // 连接池配置
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setIdleTimeout(30000);
        config.setConnectionTimeout(20000);
        config.setMaxLifetime(1800000);
        config.setPoolName("CreditSystemHikariCP");
        config.setConnectionTestQuery("SELECT 1");
        config.setAutoCommit(false);

        return new HikariDataSource(config);
    }

    private static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        return value != null && !value.isEmpty() ? value : defaultValue;
    }
}