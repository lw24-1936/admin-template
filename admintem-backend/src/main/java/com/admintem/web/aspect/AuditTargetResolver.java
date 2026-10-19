package com.admintem.web.aspect;

import java.lang.reflect.Method;

/**
 * 审计目标解析器：从被注解方法的入参中尽力识别"操作对象"。
 *
 * <p>为什么要这一层：控制器方法的入参形态不统一（有的用 IdRequest，有的用
 * UserSaveRequest，有的用 AssignRoleRequest），若在每个注解上手动声明
 * "目标字段名"，一旦 DTO 字段改名就会静默失效。这里改为按**约定 getter**
 * 自动取值：ID 优先取 {@code getId/getUserId/getRoleId/getPermissionId}，
 * 名称优先取 {@code getUserName/getRoleName/getPermissionKey/getName/getNickName}。</p>
 *
 * <p>设计取向是"尽力而为、绝不抛异常"：解析不到就返回空目标，
 * 审计记录仍然写入（动作与结果本身比目标字段更重要）。</p>
 */
public final class AuditTargetResolver {

    /** ID 取值顺序（先精确后宽泛） */
    private static final String[] ID_GETTERS = {"getId", "getUserId", "getRoleId", "getPermissionId"};

    /**
     * 名称取值顺序（账号 > 角色名 > 通用名称 > 昵称 > 权限标识）。
     *
     * <p>注意 {@code getUsername} 与 {@code getUserName} 都要列：登录请求体字段是
     * {@code username}（沿用前端契约），而用户/权限类 DTO 用的是 {@code userName}，
     * 反射按大小写精确匹配，少列一个就会出现"登录审计没有操作人"这种静默缺失。</p>
     */
    private static final String[] NAME_GETTERS = {
            "getUserName", "getUsername", "getRoleName", "getName", "getNickName", "getPermissionKey"
    };

    private AuditTargetResolver() {
    }

    /**
     * 解析方法入参中的审计目标。
     *
     * @param args 方法入参数组（可为 null）
     * @return 目标（解析不到时返回 {@link AuditTarget#EMPTY}）
     */
    public static AuditTarget resolve(Object[] args) {
        if (args == null || args.length == 0) {
            return AuditTarget.EMPTY;
        }
        for (Object arg : args) {
            if (arg == null || isInfrastructureArg(arg)) {
                continue;
            }
            String id = readFirst(arg, ID_GETTERS);
            String name = readFirst(arg, NAME_GETTERS);
            if (id != null || name != null) {
                return new AuditTarget(id, name);
            }
        }
        return AuditTarget.EMPTY;
    }

    /**
     * 是否框架参数（Servlet / 校验结果等）：这类对象可能碰巧含有同名 getter，
     * 例如 HttpServletRequest 没有 getId，但 BindingResult 之类不应被当作业务目标。
     *
     * @param arg 入参
     * @return true 表示应跳过
     */
    private static boolean isInfrastructureArg(Object arg) {
        String pkg = arg.getClass().getName();
        return pkg.startsWith("jakarta.servlet")
                || pkg.startsWith("org.springframework.web.multipart")
                || pkg.startsWith("org.springframework.validation")
                || pkg.startsWith("org.springframework.web.context.request");
    }

    /**
     * 按顺序尝试调用 getter，返回第一个非空且可转为文本的值。
     *
     * @param bean    入参对象
     * @param getters getter 名数组
     * @return 文本值；都没有则返回 null
     */
    private static String readFirst(Object bean, String[] getters) {
        for (String getter : getters) {
            try {
                Method method = bean.getClass().getMethod(getter);
                Object value = method.invoke(bean);
                if (value instanceof CharSequence text) {
                    if (!text.isEmpty()) {
                        return text.toString();
                    }
                } else if (value instanceof Number number) {
                    return number.toString();
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // 该入参没有这个属性或不可访问：继续尝试下一个 getter
            }
        }
        return null;
    }
}
