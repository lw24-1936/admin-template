package com.admintem.web.aspect;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作审计注解：标注在需要"留痕"的控制器方法上。
 *
 * <p>用法（动作码统一用 大写下划线，便于按前缀检索与统计）：</p>
 * <pre>
 * &#64;AuditLog(action = "USER_ADD", name = "新增用户", target = AuditTargetType.USER)
 * public Result&lt;Long&gt; addSysAuthUser(...) { ... }
 * </pre>
 *
 * <p>记录时机：方法执行完成后（无论成功、业务失败还是抛异常）写一条审计记录，
 * 由 {@link AuditLogAspect} 保证"审计写入失败绝不影响业务返回"。</p>
 *
 * <p>安全约定：注解只声明"这是什么动作"，具体记录哪些字段由
 * {@link AuditLogFactory} 统一决定——不记录请求体、口令、令牌等敏感内容。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {

    /**
     * 动作码（大写+下划线，如 LOGIN / USER_DELETE / ROLE_ASSIGN_PERM）。
     *
     * @return 动作码
     */
    String action();

    /**
     * 动作中文名（写库便于直接阅读，如「删除用户」）。
     *
     * @return 动作中文名
     */
    String name();

    /**
     * 操作目标类型（默认 AUTH：认证类动作）。
     *
     * @return 目标类型
     */
    AuditTargetType target() default AuditTargetType.AUTH;
}
