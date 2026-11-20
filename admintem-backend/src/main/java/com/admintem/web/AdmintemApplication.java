package com.admintem.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * admintem-backend 启动类。
 *
 * <p>技术栈：Spring Boot 3.5 + JDK 17 + Spring Security 6 + MyBatis(XML) +
 * MyBatis-Plus(分页) + Redisson + springdoc-openapi。
 * 对应数据库：MySQL 库 admintem_new（沿用旧项目），仅保留登录注册与 RBAC 权限相关能力。</p>
 *
 * <p>启动方式：{@code mvn spring-boot:run} 或执行打包后的 {@code admintem-backend.jar}；
 * 环境由 Maven Profile 切换（dev 默认/test/pre/pro），端口分别 8090/8091/8092/8093。</p>
 */
@Slf4j
@EnableScheduling
@SpringBootApplication
public class AdmintemApplication {

    /**
     * 应用入口。
     *
     * @param args 命令行参数（环境已由 Maven Profile 决定，也可用
     *             --spring.profiles.active 覆盖）
     */
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(AdmintemApplication.class, args);
        // 端口从运行环境读取：System.getProperty("server.port") 并未设置，
        // 写死默认值会让 test/pre/pro（8091/8092/8093）的启动日志显示成 8090
        String port = context.getEnvironment().getProperty("server.port", "8090");
        log.info("================== admintem-backend 启动成功 ==================");
        log.info("接口文档: http://localhost:{}/swagger-ui/index.html", port);
    }
}
