package com.admintem.web.config.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * springdoc-openapi 文档配置。
 *
 * <p>访问地址（dev）：<a href="http://localhost:8090/swagger-ui/index.html">Swagger UI</a></p>
 *
 * <p><b>认证声明（B4 新增）</b>：本项目用自定义请求头
 * {@code admintem_token: "LAUNCH <jwt>"}（沿用旧项目约定），不是标准的
 * {@code Authorization: Bearer}。不声明时 Swagger 里调受保护接口只能手工加请求头，
 * 很容易漏掉前缀——"接口调不通"里有相当一部分其实是没带令牌。</p>
 *
 * <p><b>使用步骤</b>：① 调 {@code POST /auth/login} 取 {@code data.token}；
 * ② 点右上角 Authorize，填 {@code LAUNCH <token>}（含前缀与空格）；
 * ③ 之后所有请求自动携带该头。</p>
 *
 * <p>这里只影响文档 UI，不改变运行时鉴权（见 {@code SecurityConfig} 与
 * {@code JwtAuthenticationFilter}）。</p>
 */
@Configuration
public class OpenApiConfig {

    /** 安全方案名：与 {@code @SecurityScheme} 的 name 对应 */
    private static final String SCHEME_NAME = "admintem_token";

    /**
     * 组装 OpenAPI 文档基础信息与安全方案。
     *
     * @return OpenAPI
     */
    @Bean
    public OpenAPI admintemOpenApi() {
        // 请求头认证方案：类型 APIKEY + 位置 HEADER。
        // 注意 swagger-models 的 SecurityScheme 没有 paramName()：APIKEY 的“参数名”
        // 就是 name 字段本身（这里 name = admintem_token，即请求头名称）。
        SecurityScheme tokenScheme = new SecurityScheme()
                .name(SCHEME_NAME)
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .description("填 \"LAUNCH <登录返回的 token>\"（注意包含 LAUNCH 前缀与一个空格）");

        return new OpenAPI()
                .info(new Info()
                        .title("admintem-backend 权限认证接口文档")
                        .description("Spring Boot 3.5 + Vue3 重构版（登录/注册/用户/角色/权限管理，含按钮级权限）。"
                                + "统一响应 {code,message,data}；401 未登录、403 无权限、"
                                + "业务失败为 HTTP 200 + 业务码（5xx 段）。")
                        .version("1.0.0")
                        .contact(new Contact().name("admintem-work-new 团队")))
                .components(new Components().addSecuritySchemes(SCHEME_NAME, tokenScheme))
                // 全局默认要求该安全方案：Swagger UI 会为每个接口显示小锁并可一键填充
                .addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME));
    }
}
