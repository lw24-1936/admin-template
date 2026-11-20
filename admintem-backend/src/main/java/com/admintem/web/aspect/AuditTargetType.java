package com.admintem.web.aspect;

/**
 * 审计目标类型：说明"这次操作作用在什么对象上"，用于按类型筛选审计记录。
 */
public enum AuditTargetType {

    /** 认证类（登录、登出、注册） */
    AUTH,

    /** 本人操作（修改自己的资料/密码） */
    SELF,

    /** 用户对象 */
    USER,

    /** 角色对象 */
    ROLE,

    /** 权限（菜单/按钮）对象 */
    PERMISSION,

    /** 系统参数配置对象 */
    CONFIG,

    /** 数据字典对象 */
    DICT,

    /** 文件对象 */
    FILE,

    /** 访问控制（越权被拒绝，无具体业务对象） */
    ACCESS
}
