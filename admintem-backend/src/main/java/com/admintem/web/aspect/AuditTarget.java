package com.admintem.web.aspect;

/**
 * 审计目标（从方法入参里解析出来的"操作对象"）。
 *
 * @param id   目标ID（用户ID/角色ID 等；可能为空）
 * @param name 目标名称（账号/角色名/权限名；可能为空）
 */
public record AuditTarget(String id, String name) {

    /** 未解析到目标时使用（不写 null 判断，避免各处空指针） */
    public static final AuditTarget EMPTY = new AuditTarget(null, null);

    /** 是否为空目标 */
    public boolean isEmpty() {
        return id == null && name == null;
    }
}
