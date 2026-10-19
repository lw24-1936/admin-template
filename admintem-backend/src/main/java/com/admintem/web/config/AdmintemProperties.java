package com.admintem.web.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务配置绑定类（对应 application.yml 中前缀 {@code admintem}）。
 *
 * <p>集中承载 JWT、注册策略、Redis key 前缀、超级角色等可调参数，
 * 避免散落的魔法字符串；修改配置后需同步维护 docs 下设计文档。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "admintem")
public class AdmintemProperties {

    /** JWT 相关配置 */
    private Jwt jwt = new Jwt();

    /** 认证相关配置 */
    private Auth auth = new Auth();

    /** Redis 缓存相关配置 */
    private Cache cache = new Cache();

    /** 安全相关配置 */
    private Security security = new Security();

    /** SQL 日志/慢 SQL 配置 */
    private Sql sql = new Sql();

    /** 请求跟踪（traceId）配置 */
    private Log log = new Log();

    /** 操作审计配置 */
    private Audit audit = new Audit();

    /** 文件上传（头像等），见 {@link Upload} */
    private Upload upload = new Upload();

    /* ------------------------------------------------------------------ */

    /**
     * JWT 令牌配置段。
     */
    @Data
    public static class Jwt {
        /** HS256 签名密钥（至少 32 字节） */
        private String secret;

        /** 普通登录令牌有效期（分钟） */
        private long expireMinutes = 180;

        /** 勾选记住我后的令牌有效期（天） */
        private long rememberExpireDays = 7;

        /** 令牌请求头名称 */
        private String header = "admintem_token";

        /** 令牌前缀 */
        private String bearerPrefix = "LAUNCH ";

        /** Redis 中令牌白名单 key 前缀 */
        private String redisKeyPrefix = "admintem:auth:jwt:";

        /**
         * 生产环境是否拒绝"示例默认密钥"。
         *
         * <p>示例密钥里带 {@code change-me} 标记，仅供本地开发使用。
         * 生产（profile 含 pro）若仍在使用它，直接启动失败——
         * 这类"忘了换密钥"的配置事故不会自己暴露，只会在被伪造令牌时才被发现。</p>
         */
        private boolean failOnDefaultSecret = true;
    }

    /**
     * 认证（登录/注册/锁定）配置段。
     */
    @Data
    public static class Auth {
        /** 是否开放自助注册 */
        private boolean registerEnabled = true;

        /** 注册用户默认角色 role_key */
        private String defaultRoleKey = "common";

        /** 连续登录失败最大次数 */
        private int maxLoginFailCount = 5;

        /** 失败锁定分钟数 */
        private int loginLockMinutes = 10;

        /** 密码策略（B4） */
        private Password password = new Password();

        /**
         * 密码策略。
         */
        @Data
        public static class Password {
            /**
             * BCrypt 代价因子（4~31）。
             *
             * <p>默认 10（Spring Security 默认）。越高越难暴力破解，但登录耗时线性增加：
             * 代价因子每 +1，单次校验耗时翻倍。生产可提到 12，需实测登录 P99。</p>
             */
            private int bcryptStrength = 10;

            /**
             * 登录成功时是否把旧散列自动升级到当前代价因子。
             *
             * <p>BCrypt 的代价因子写死在散列里；提高配置值只影响新密码，
             * 老用户仍是旧强度。开启后，用户下次登录（此时明文在手）即被静默升级。</p>
             */
            private boolean rehashOnLogin = true;

            /** 是否启用复杂度校验（默认关闭，避免影响既有演示账号与接口回归） */
            private boolean complexityEnabled = false;

            /** 复杂度正则（默认：至少 8 位且同时包含字母与数字） */
            private String complexityRegex = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$";

            /** 复杂度不满足时的提示 */
            private String complexityMessage = "密码至少 8 位，且需同时包含字母与数字";
        }

        /** Redis 登录失败计数 key 前缀 */
        private String failCountKeyPrefix = "admintem:auth:fail:";

        /** Redis 账号锁定 key 前缀 */
        private String lockKeyPrefix = "admintem:auth:lock:";

        /* ---------------- IP 维度风控（D4，默认关闭） ---------------- */

        /**
         * 是否启用"按来源 IP"的登录风控。
         *
         * <p>默认**关闭**：本地联调、演示环境经常在同一台机器上反复用错密码，
         * 打开会把开发者自己挡住；生产环境建议开启（见《04》配置项）。</p>
         */
        private boolean ipLimitEnabled = false;

        /** 同一 IP 连续登录失败上限（达到后临时限制该 IP 登录） */
        private int ipMaxLoginFailCount = 20;

        /** IP 维度限制时长（分钟） */
        private int ipLockMinutes = 10;

        /** Redis：IP 维度失败计数 key 前缀 */
        private String ipFailCountKeyPrefix = "admintem:auth:fail:ip:";

        /** Redis：IP 维度限制 key 前缀 */
        private String ipLockKeyPrefix = "admintem:auth:lock:ip:";
    }

    /**
     * Redis 缓存配置段。
     */
    @Data
    public static class Cache {
        /** 用户权限快照 key 前缀 */
        private String userPermKeyPrefix = "admintem:auth:user:perm:";

        /** 权限快照有效期（分钟） */
        private long userPermExpireMinutes = 30;
    }

    /**
     * 安全配置段。
     */
    @Data
    public static class Security {
        /** 超级角色 role_key（拥有全部权限） */
        private String adminRoleKey = "admin";

        /** 免登录白名单（Ant 风格路径） */
        private List<String> permitUrls = new ArrayList<>();

        /**
         * 允许跨域的前端来源（Ant 风格通配，逗号分隔）。
         *
         * <p>开发/测试默认放行本机端口（桌面端 5173、移动端 5174 等）；
         * 生产通过 {@code ADMINTEM_CORS_ORIGINS} 显式给出实际域名，留空表示不放开任何跨域来源
         * （推荐生产由 Nginx 同源反代，从根上避免跨域）。</p>
         */
        private String corsAllowedOrigins = "http://localhost:*,http://127.0.0.1:*";
    }

    /**
     * SQL 日志配置段（参考 config/mybatis 的日志设计，便于排查与慢 SQL 定位）。
     */
    @Data
    public static class Sql {
        /** 是否打印全量带参 SQL（mapperId + 参数替换后语句；生产建议 false） */
        private boolean logEnable = true;

        /** 慢 SQL 阈值（毫秒），超过则 WARN 到 sys-slow-sql（logs/sql/admintem-slow.log） */
        private long slowMs = 2000;

        /**
         * 按 mapperId（可写 Mapper 名或方法名前缀）区分的慢 SQL 阈值（毫秒）。
         *
         * <p>用于把热路径收紧、把重查询放宽，例如：
         * <pre>
         * admintem:
         *   sql:
         *     slow-ms-by-mapper:
         *       SysAuthUserMapper.selectByUserName: 300   # 登录热路径
         *       SysAuthPermissionMapper.selectAllOrdered: 500
         * </pre>
         * 匹配规则为"最长包含匹配"，未命中的语句使用 {@link #slowMs}。</p>
         */
        private Map<String, Long> slowMsByMapper = new LinkedHashMap<>();

        /**
         * 是否对全量 SQL 日志中的敏感字段脱敏（手机号/邮箱/证件号/口令等）。
         *
         * <p>默认开启：开发联调同样不建议把真实个人信息写进日志文件；
         * 如需排查某个字段的真实取值，可临时改为 false 并知悉风险。</p>
         */
        private boolean maskEnable = true;
    }

    /**
     * 请求跟踪配置段（参考 config/log 的 Filter 设计）。
     */
    @Data
    public static class Log {
        /** 是否生成并透传 traceId（X-Trace-Id，贯穿请求与 SQL 日志） */
        private boolean traceEnable = true;

        /** 响应头中的 traceId 名称 */
        private String traceHeader = "X-Trace-Id";
    }

    /**
     * 操作审计配置段（落库留痕，见 02 设计文档第 13 节）。
     *
     * <p>审计被刻意设计为"可随时关闭、且失败不影响业务"：关闭后切面不注册、
     * 403 出口也不写库，业务行为完全不变。</p>
     */
    @Data
    public static class Audit {
        /** 是否启用操作审计（写 sys_auth_audit_log） */
        private boolean enable = true;

        /** 失败原因等长文本的截断长度（避免超长导致插入失败） */
        private int maxTextLength = 200;

        /** 审计日志保留天数（仅用于运维清理 SQL 的默认值，程序不做自动删除） */
        private int retentionDays = 180;

        /** 是否记录"越权被拒绝（403）"的尝试（安全审计信号，建议开启） */
        private boolean logDenied = true;
    }

    /**
     * 上传配置（头像等小文件）。
     *
     * <p>本项目此前没有任何文件存储，头像功能是第一个；存储方案刻意选"本地目录"：
     * 部署时挂载一个卷即可，不引入对象存储依赖。若后续要做多实例部署，
     * 把 {@link #dir} 指向共享存储（NFS/OSS 挂载点）即可，接口层不用改。</p>
     */
    @Data
    public static class Upload {
        /** 头像存放目录（相对路径按进程工作目录解析） */
        private String avatarDir = "./uploads/avatar";

        /** 单文件大小上限（MB） */
        private int maxSizeMb = 2;

        /** 对外访问前缀（与 WebMvcConfig 的资源映射、前端拼 URL 保持一致） */
        private String urlPrefix = "/uploads/avatar";

        /** 允许的扩展名（同时会做文件头校验，防止改扩展名上传可执行文件） */
        private String allowedExtensions = "png,jpg,jpeg,webp,gif";
    }}
