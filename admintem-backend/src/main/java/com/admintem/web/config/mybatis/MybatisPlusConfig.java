package com.admintem.web.config.mybatis;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.admintem.web.config.AdmintemProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置（参考 config/mybatis 的分层设计）。
 *
 * <p>注册：
 * <ul>
 *   <li>分页插件（MySQL 方言，单页上限 500）；</li>
 *   <li>全量 SQL 日志 {@link SqlLogInnerInterceptor}（开关 admintem.sql.log-enable，
 *       默认 true；生产建议 false，仅保留慢 SQL）；</li>
 *   <li>慢 SQL 统计 {@link SqlSlowInterceptor}（org.apache.ibatis.plugin.Interceptor
 *       Bean 会被 mybatis-spring-boot 自动注入 Executor，阈值 admintem.sql.slow-ms）。</li>
 * </ul>
 * 逻辑删除由 Mapper XML 显式维护（见 resources/mapper/*.xml）。</p>
 */
@Configuration
@MapperScan("com.admintem.web.mapper")
public class MybatisPlusConfig {

    /**
     * 分页插件 + 全量 SQL 日志（InnerInterceptor 链）。
     *
     * @param properties 业务配置（SQL 日志开关、脱敏开关）
     * @return MybatisPlusInterceptor
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(AdmintemProperties properties) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 1. 全量 SQL 日志（按配置开关；参数值按敏感字段脱敏，避免个人信息落盘）
        if (properties.getSql().isLogEnable()) {
            interceptor.addInnerInterceptor(new SqlLogInnerInterceptor(properties.getSql().isMaskEnable()));
        }
        // 2. 分页插件：为 Page 对象自动拼接 LIMIT
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        // 单页最大 500 条，防止恶意超大分页拖垮数据库
        pagination.setMaxLimit(500L);
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }

    /**
     * 慢 SQL 统计（Executor 插件，mybatis-spring-boot 自动装配）。
     *
     * @param properties 业务配置（默认真阈值 + 按 mapper 的分级阈值）
     * @return SqlSlowInterceptor
     */
    @Bean
    public SqlSlowInterceptor sqlSlowInterceptor(AdmintemProperties properties) {
        return new SqlSlowInterceptor(properties.getSql().getSlowMs(),
                properties.getSql().getSlowMsByMapper());
    }
}
