# admintem-backend · 权限认证服务端

基于 **Spring Boot 3.5 + JDK 17** 的 RBAC 权限认证服务，提供登录、注册、用户管理、角色管理、
权限管理（菜单 + 按钮）等能力，配套 Vue 3 前端 `admintem-vue` 组成完整的权限管理系统。

- 应用端口：`8090`（dev） / `8091`（test） / `8092`（pre） / `8093`（pro）
- 接口文档：http://localhost:8090/swagger-ui/index.html
- 后端代码根包：`com.admintem.web`

---

## 一、功能特性

| 模块 | 能力 |
| ---- | ---- |
| 认证 | 登录（记住我）、登出（服务端令牌即时失效）、自助注册（自动分配默认角色） |
| 会话 | 当前用户信息（角色 / 菜单树 / 权限标识集合）、个人资料与密码修改 |
| 用户管理 | 分页查询、新增、编辑、逻辑删除、重置密码、分配角色 |
| 角色管理 | 查询全部、分页、新增、编辑、删除、树形分配权限 |
| 权限管理 | 菜单 / 按钮两级维护（新增子级、编辑、级联删除）、类型与路由字段管理 |
| 权限控制 | 菜单权限控页面可见、按钮权限控操作可用；前后端统一 `permission_key` |
| 安全 | BCrypt 密码、JWT + Redis 白名单、登录失败锁定、内置管理员保护、逻辑删除 |
| 可观测 | 请求 traceId 全链路、全量带参 SQL、慢 SQL 独立日志、接口调用耗时切面 |
| 多环境 | Maven Profile 一键切换 dev / test / pre / pro |

## 二、技术栈

| 组件 | 版本 / 说明 |
| ---- | ---- |
| JDK | 17 |
| Spring Boot | 3.5.16 |
| Spring Security | 6（无状态 JWT + 方法级鉴权） |
| 持久层 | MyBatis（SQL 全部 XML）+ MyBatis-Plus（仅分页插件） |
| 数据库 | MySQL 8.x，库 `admintem_new` |
| 缓存 / 会话 | Redis 7.x，客户端 Redisson 3.52.0 |
| 令牌 | jjwt 0.12.6（HS256） |
| 接口文档 | springdoc-openapi 2.9.1（Swagger UI） |
| 构建 | Maven 3.9 |

## 三、目录结构

```
admintem-backend
├─ src/main/java/com/admintem/web
│  ├─ AdmintemApplication.java        启动类
│  ├─ config                       配置
│  │  ├─ security/                 SecurityConfig
│  │  ├─ web/                      CorsConfig、OpenApiConfig
│  │  ├─ mybatis/                  分页插件、SQL 日志、慢 SQL 统计
│  │  ├─ redis/                    RedissonConfig、RedissonProperties
│  │  └─ log/                      过滤器注册（traceId）
│  │  └─ AdmintemProperties.java     业务配置绑定（admintem.*）
│  ├─ security/                    LoginUser、UserDetailsService、JWT 过滤器、RbacService、Redis 缓存
│  ├─ filter/                      RequestTraceFilter（traceId 全链路）
│  ├─ controller/                  Auth、用户、角色、权限、关联关系
│  ├─ service/ + service/impl/     认证与业务服务
│  ├─ mapper/                      5 个 Mapper 接口（SQL 在 resources/mapper）
│  ├─ entity/ + entity.dto/ + entity.vo/   实体、请求体、响应视图
│  ├─ common/ exception/ aspect/ constant/ util/  统一返回、异常、切面、常量、工具
├─ src/main/resources
│  ├─ application.yml              通用配置（环境由 Maven Profile 注入）
│  ├─ application-{dev,test,pre,pro}.yml
│  ├─ mapper/*.xml                 5 张表的全部 SQL
│  └─ logback-spring.xml           日志与 SQL/慢 SQL 文件
└─ pom.xml
```

## 四、环境要求

- JDK 17（示例：`C:\dev\Java\jdk17`）
- Maven 3.9+
- MySQL 8.x（可访问目标库）
- Redis 6/7（可访问目标实例）

## 五、快速开始

```powershell
cd admintem-backend
$env:JAVA_HOME = 'C:\dev\Java\jdk17'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

mvn spring-boot:run            # dev，端口 8090
```

打包运行：

```powershell
mvn -DskipTests clean package
java -jar target/admintem-backend.jar
```

### 多环境切换（Maven Profile）

| Profile | 配置文件 | 端口 | 命令 |
| ---- | ---- | ---- | ---- |
| dev（默认） | application-dev.yml | 8090 | `mvn spring-boot:run` |
| test | application-test.yml | 8091 | `mvn -P test spring-boot:run` |
| pre | application-pre.yml | 8092 | `mvn -P pre spring-boot:run` |
| pro | application-pro.yml | 8093 | `mvn -P pro -DskipTests package` |

原理：`pom.xml` 中 4 个 profile 注入 `env-flag`，配合资源过滤把 `application.yml` 里的
`@env-flag@` 替换为实际环境名，从而激活对应的 `application-{env}.yml`。

## 六、配置项（application.yml，前缀 `admintem`）

| 配置项 | 默认 | 说明 |
| ---- | ---- | ---- |
| `admintem.jwt.secret` | 示例值 | HS256 签名密钥（≥32 字节，生产必须替换） |
| `admintem.jwt.expire-minutes` | 180 | 普通登录令牌有效期（分钟） |
| `admintem.jwt.remember-expire-days` | 7 | 记住我令牌有效期（天） |
| `admintem.auth.register-enabled` | true | 是否开放自助注册（生产建议 false） |
| `admintem.auth.default-role-key` | common | 注册用户默认角色 |
| `admintem.auth.max-login-fail-count` | 5 | 连续登录失败锁定阈值 |
| `admintem.auth.login-lock-minutes` | 10 | 锁定时长（分钟） |
| `admintem.security.admin-role-key` | admin | 超级角色标识（自动放行全部权限） |
| `admintem.security.permit-urls` | 登录/注册 | 免登录白名单 |
| `admintem.sql.log-enable` | true | 是否输出全量带参 SQL（生产 false） |
| `admintem.sql.slow-ms` | 2000 | 慢 SQL 阈值（毫秒） |
| `admintem.log.aspect.enable` | true | 接口调用日志切面开关 |
| `admintem.log.trace-enable` | true | traceId 过滤器开关 |
| `redisson.*` | 见各环境 yml | Redis 地址、密码、库号、连接池 |

## 七、数据库

库名 `admintem_new`，共 5 张表：

| 表 | 说明 |
| ---- | ---- |
| `sys_auth_user` | 用户（BCrypt 密码、状态、逻辑删除） |
| `sys_auth_role` | 角色（`role_key` 唯一标识，`admin` 为超级角色） |
| `sys_auth_permission` | 权限（`type=0` 菜单 / `type=1` 按钮，含 `permission_key`） |
| `sys_auth_user_role` | 用户-角色关联 |
| `sys_auth_role_permission` | 角色-权限关联 |

初始化脚本（位于 `../admintem-docs/db/`）：

```bash
# 一键全量初始化（重建 5 表 + 基线数据：admin/test 账号、3 角色、18 权限）
mysql -h<host> -P3306 -uroot -p admintem_new < ../admintem-docs/db/init/00_admintem_full_init.sql

# 增量脚本目录
../admintem-docs/db/changelog/
```

演示账号：`admin / 12345678`（管理员）、`test / 12345678`（仅有查询权限）。

## 八、Redis 设计

所有键统一前缀 `admintem:auth:`，由 Redisson 客户端访问：

| 键 | 内容 | TTL |
| ---- | ---- | ---- |
| `admintem:auth:jwt:{userName}` | 令牌白名单（服务端可强制下线） | 与令牌一致（3h / 7d） |
| `admintem:auth:user:perm:{userId}` | 登录用户权限快照（角色 + permission_key 集合） | 30 分钟 |
| `admintem:auth:fail:{userName}` | 登录失败计数 | 10 分钟 |
| `admintem:auth:lock:{userName}` | 账号锁定标记 | 10 分钟 |

角色 / 权限 / 用户变更时会主动清理受影响用户的快照，使权限调整即时生效。

## 九、权限模型

- **菜单权限（type=0）**：控制页面与左侧导航是否可见；`component` 字段为前端 `views` 相对路径；
- **按钮权限（type=1）**：控制页面内操作按钮与对应写接口；
- **统一标识**：`permission_key`（如 `sysAuthUser:addSysAuthUser`）同时用于
  后端注解与前端指令，保证两端一致。

后端用法示例：

```java
@PostMapping("/addSysAuthUser")
@PreAuthorize("@rbac.hasPerm('sysAuthUser:addSysAuthUser')")
public Result<Long> addSysAuthUser(@Valid @RequestBody UserSaveRequest request) { ... }
```

内置保护规则：内置管理员不可删除/禁用、其 `admin` 角色不可移除、内置角色不可删除、
角色存在绑定用户时不可删除、权限删除会级联清理子级与授权关系。

## 十、日志与慢 SQL

日志目录 `logs/`（可用 `-DLOG_HOME` 覆盖）：

| 文件 | 内容 |
| ---- | ---- |
| `logs/admintem-backend-info.log` | 全量 INFO+（异步写入，按天/大小滚动 .gz，保留 30 天） |
| `logs/admintem-backend-error.log` | 仅 ERROR |
| `logs/sql/admintem-sql.log` | 全量带参 SQL（dev/test/pre 开启） |
| `logs/sql/admintem-slow.log` | 慢 SQL（默认 >2000ms，WARN 级） |

每条日志含 `[traceId]`，响应头返回 `X-Trace-Id`，可将「一次请求 → 多条 SQL」串联排查。

## 十一、接口一览

| 分组 | 主要接口 |
| ---- | ---- |
| 认证 `/auth` | `login`、`register`、`logout`、`getUserInfo`、`updateProfile`、`updatePassword` |
| 用户 `/sysAuthUser` | `paginQuery`、`addSysAuthUser`、`editSysAuthUser`、`deleteById`、`resetPassword`、`assignRole` |
| 角色 `/sysAuthRole` | `queryAll`、`paginQuery`、`addAuthRole`、`editAuthRole`、`deleteById`、`assignPermission` |
| 权限 `/sysAuthPermission` | `treeList`、`addAuthPermission`、`editAuthPermission`、`deleteById` |
| 关联 | `/sysAuthUserRole/getUserRoleByUserId`、`/sysAuthRolePermission/getPermissionByRoleId` |

统一响应结构：`{ "code": 200, "message": "操作成功", "data": ... }`；
认证失败返回 HTTP 401，权限不足返回 HTTP 403。

## 十二、测试

```powershell
# 后端全量接口回归（60 项断言，需服务运行在 8090）
cd ../admintem-docs/tests
pwsh api-regression-test.ps1

# 清理测试数据
mysql ... admintem_new < cleanup-test-data.sql
```

回归覆盖：登录 / 注册 / 登出、用户 / 角色 / 权限 CRUD、按钮级 403 越权、令牌吊销、
失败锁定、内置保护规则等。

## 十三、常见问题

| 现象 | 处理 |
| ---- | ---- |
| 启动报 Redis 连接超时 | 确认 Redis 可达：`Test-NetConnection <host> -Port 6379`；检查 `redisson.address` 与密码 |
| 登录返回 501 | 账号或密码错误；连续 5 次失败将返回 502 锁定 |
| 接口返回 401 | 令牌过期或被吊销（登出 / 改密 / 重置 / 同账号重登）；重新登录即可 |
| 接口返回 403 | 当前账号缺少对应 `permission_key`，在「角色管理 → 分配权限」中授权 |
| 需要看 SQL | 查看 `logs/sql/admintem-sql.log`；慢查询看 `admintem-slow.log` |
| 中文乱码 | 请求头使用 `Content-Type: application/json; charset=utf-8` |

## 十四、文档索引

完整设计与操作文档位于 `../admintem-docs/`：需求范围、系统设计、数据库、部署操作、
接口与权限对照、测试报告、维护约定、动态认证说明、代码审计、功能扩展规划。
