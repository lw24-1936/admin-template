# AGENTS.md

> 给在本仓库工作的 AI/自动化代理的项目级约定。**先读本文件，再读 `README.md` 与 `admintem-docs/` 对应文档。**
> 代码、数据库、权限、页面、测试与文档必须同步修改；不要把“计划中”写成“已完成”。

## 1. 项目定位

`admintem` 是一个可二次开发的后台管理框架模板：

- 后端：Spring Boot 3.5 + JDK 17 + Spring Security 6 + MyBatis(XML) + MyBatis-Plus + Redisson + Flyway + springdoc。
- 桌面端：Vue 3 + Vite + Element Plus + Pinia + Vue Router。
- 移动端：Vue 3 + Vite + Vant 4。
- uni-app：Vue 3 + Vite 5（H5 / 小程序 / App）。
- 官网模板：Vue 3 + Vite 5。
- 核心能力：JWT + Redis 白名单、refreshToken 轮换、多设备会话（deviceId）、菜单/按钮/接口三层 RBAC、行级数据权限、
  审计、在线会话、部门、系统参数、数据字典、文件中心、用户 CSV 导入导出、动态路由、桌面端深色主题。
- 当前已知缺口见 `admintem-docs/10-后续功能扩展规划.md`，不要重复实现已标记完成的能力。

## 2. 常用命令

在仓库根目录执行：

```bash
# 一键启动（MySQL/Redis/后端/桌面/移动，推荐）
cp .env.example .env
docker compose up -d --build

# 只跑质量门禁（不依赖真实中间件）
mvn -f admintem-backend/pom.xml test
npm --prefix admintem-vue run lint
npm --prefix admintem-vue test
npm --prefix admintem-mobile test

# 文档/权限检查（提交前必须全绿）
node admintem-docs/tests/check-doc-images.cjs
node admintem-docs/tests/check-tokens.cjs
node admintem-docs/tests/check-perm-keys.cjs
node admintem-docs/tests/check-permission-doc-consistency.cjs
node admintem-docs/tests/gen-role-endpoint-matrix.cjs --check

# 后端启动后导出 OpenAPI + 生成 TS 类型
node scripts/export-openapi.mjs
npx --yes openapi-typescript@7 admintem-docs/api/openapi.json -o admintem-docs/api/schema.d.ts
```

本地手动启动：

```bash
# 后端：默认 8090，dev profile
cd admintem-backend && mvn spring-boot:run

# 桌面 5175 / 移动 5174 / uni-app 5176 / 官网 5177
cd admintem-vue && npm run dev
cd admintem-mobile && npm run dev
cd admintem-uniapp && pnpm dev:h5
cd admintem-site && npm run dev
```

默认账号：`admin / 12345678`（管理员）、`test / 12345678`（受限）。

## 3. 目录与责任边界

```text
admintem-backend/       Spring Boot 后端；Controller/Service/Mapper/XML 分层
admintem-vue/           桌面端（Element Plus，动态路由 + v-perm）
admintem-mobile/        移动端（Vant，H5）
admintem-uniapp/        uni-app 多端
admintem-site/          官网模板
admintem-docs/          设计、数据库、接口、部署、测试、截图
  db/init/              全量初始化（含显式确认闸门）
  db/changelog/         Flyway 增量迁移（只新增，不修改历史）
  tests/                API/E2E/a11y/文档检查脚本
docker-compose.yml      本地一键编排
```

## 4. 后端约定

1. **接口返回**统一 `Result<T>`；业务异常 `BusinessException` + `ResultCode`，不要直接返回裸对象或手写错误 JSON。
2. **权限**：
   - 菜单/按钮权限在 `sys_auth_permission.permission_key` 定义；
   - 接口用 `@PreAuthorize("@rbac.hasPerm('xxx')")`，并同步《05》与初始化种子；
   - 超级角色 `admin` 自动放行；不要绕过 `@PreAuthorize` 在 Controller 里手写角色判断。
3. **审计**：关键写操作加 `@AuditLog(action=..., name=..., target=...)`；审计失败不能影响业务。
   配置/字典等显式变更可用 `AuditLogService.recordDataChange` 写已脱敏 before/after JSON，禁止写入密码/令牌原文。
4. **分页**：请求 DTO 继承 `PageQuery`，Controller 参数加 `@Valid`，否则 `pageNum/pageSize` 约束不生效。
5. **API 版本化**：`/api/v1/**` 由 `ApiVersionFilter` 剥离前缀后复用旧路由；新增接口无需重复注册 v1 路径，
   但不要依赖 v1 做破坏性变更，正式废弃旧路径需单独评审。
6. **数据权限**：需要行级过滤的查询复用 `DataScopeService` 的语义（ALL/DEPT/DEPT_AND_CHILD/SELF），
   条件必须由服务端注入；`UserPageQuery` 的 `dataScope*` 字段禁止客户端伪造。
7. **认证**：请求头 `admintem_token: LAUNCH <accessToken>`；登录返回 access + refresh；
   `/auth/refresh` 为公开端点；客户端登录应带 `deviceId`，后端按 `{userName}:{deviceId}` 分设备保存访问令牌；
   不传 `deviceId` 保持旧版单账号单令牌语义。退出只踢当前设备，改密/禁用/全部下线才清全部设备；
   强制下线可按设备操作，refreshToken 也绑定设备。 MFA 开启后密码登录返回 `mfaToken`，必须调 `/auth/mfa/verify` 才签发正式令牌；TOTP secret 不得出现在日志/普通查询。 密码找回默认 log 模式只允许本地开发，生产必须切 `admintem.auth.password-reset.mode=smtp`；验证码不得写入接口响应。 SSO 只接受受信网关的 HMAC JWT 断言（iss/aud/jti/exp），secret 至少 32 字节且不得入库/入日志。
8. **文件存储**：通过 `FileStorageService`/`StorageStrategy`，不要在业务层直接写本地路径或 MinIO SDK。
9. **配置**：DB/Redis/MinIO 使用环境变量（见 `.env.example` 与 `config/application-local.yml.example`）；
   生产禁止默认密钥/示例密码/root 数据库账号。
9. **时间/日志**：Java 侧使用 `java.time`；日志包含 `traceId`；敏感字段必须脱敏，不要打印密码/Token。
10. **构建**：改动后至少跑 `mvn test`；涉及 SQL 的改动必须验证迁移在“全量初始化 + Flyway”与“已有库升级”两条路径都可用。

## 5. 数据库迁移约定

- 命名：`V1.0.N__description.sql`，递增，不修改已发布脚本；当前最新为 `V1.0.16`。
- **已发布迁移不可改**：CI 会先执行 `db/init/00_admintem_full_init.sql`，再跑 Flyway。
  新增列只写在迁移中，不要提前加进 init 的 CREATE TABLE（否则 V1.0.11 这类非幂等 ALTER 会重复执行）。
  新增表/权限的迁移应使用 `IF NOT EXISTS` / `ON DUPLICATE KEY UPDATE`，保证 init+Flyway 双路径可重复。
- 同步修改：
  - `admintem-docs/db/init/00_admintem_full_init.sql`（新库完整结构 + 种子）
  - `admintem-docs/03-数据库设计与变更说明.md`（变更说明）
  - 如新增权限：同步 `admintem-docs/05-接口与权限标识对照表.md` 与角色矩阵生成器
- 删除/重建表的初始化脚本有 `@admintem_confirm_reset` 闸门，不要去掉。

## 6. 前端约定

- 桌面端页面放在 `admintem-vue/src/views/<component>/index.vue`；后端菜单 `component` 使用
  `views` 相对路径（如 `sysConfig/index`），前端通过 `import.meta.glob` 动态注册，**不需要改路由表**。
- 按钮权限统一 `v-perm="'permission:key'"`；无权限时 fail-closed，不要用 `v-if` 手写角色判断。
- 列表/弹层优先复用 `ProTable.vue`（表格 + 分页 + 可选多选）与 `FormDialog.vue`（统一弹层与默认确认/取消），不要每页重复拼装。
- Element Plus 采用按需注册：新增组件时同时补 `main.js` 组件数组与样式导入；菜单动态图标补
  `main.js` 图标注册与 `MenuItem.vue` 的 `ICON_MAP`。
- API 统一走 `src/utils/request.js`；收到 401 时会尝试 refreshToken 并重放一次，登录/刷新请求本身不重试。
- 主题：使用 `--lc-*` token；深色主题通过 `data-theme="dark"` + `html.dark`，不要写死浅色背景。
- i18n：桌面端文案优先用 `@/i18n` 的 `t('key')`，菜单名用 `translateMenuName`；当前只覆盖外壳/登录/错误页，业务页新增文案要登记 key。
- 移动端/uni-app 保持与桌面端相同的认证头、返回体和权限标识；移动端已有 `/users` 用户列表（查询 + 启停），其余完整管理操作仍以桌面端为主。 uni-app 已对齐 401→refreshToken 轮换与并发刷新去重。

## 7. 测试与验收

- 后端单测：`mvn -f admintem-backend/pom.xml test`（当前 170 项）。
- 代码生成器：`node scripts/generate-crud.mjs --spec scripts/generate-crud.spec.example.json --out tmp/generated`；
  默认只写 `--out`，不覆盖源码，生成后按《17-代码生成器使用说明》人工复核权限/菜单/迁移版本。
- 桌面 lint/build：`npm --prefix admintem-vue run lint`、`npm --prefix admintem-vue run build`。
- 移动 build：`npm --prefix admintem-mobile run build`。
- 文档六件套（配图/tokens/权限标识/05 一致性/矩阵/OpenAPI）与角色矩阵：见第 2 节命令；接口/权限变化后必须先跑生成器再 `--check`。
- 完整回归：`.github/workflows/e2e-nightly.yml`（MySQL/Redis + 78 接口 + 21 桌面 E2E + 44 移动 E2E）。
- **修改测试基线时以实际输出为准**，不要手写估值；历史数据保留时在文档中标注“历史快照”。

## 8. 部署

- 本地优先 `docker compose up -d --build`；生产参考 `admintem-docs/04-部署与操作手册.md`。
- 生产 profile 会关闭 Swagger/Flyway 自动迁移等敏感能力，并强制 JWT 密钥；不要关闭这些保护。
- Nginx 已带基础安全响应头；新增静态站点时保持同源反代 `/auth`、`/sysAuth*`、`/uploads`、`/actuator/health`。
- 不提交 `.env`、`application-local.yml`、上传文件、日志、数据库备份。

## 9. 文档同步（硬要求）

改以下内容时，必须同步对应文档并跑检查：

| 改动 | 必须同步 |
| ---- | ---- |
| 接口/权限标识 | `05-接口与权限标识对照表.md`、矩阵生成器、`check-perm-keys` |
| 表结构/迁移 | `03-数据库设计与变更说明.md`、`db/init`、初始化验证 |
| 页面/按钮/字段 | `12-页面与界面说明.md`、截图（如变化） |
| 测试数量/基线 | `06-测试报告.md`、`README.md`、文档中心 |
| 部署/环境变量 | `04-部署与操作手册.md`、`.env.example` |
| 新能力/边界 | `01`、`02`、`10` 规划、根 README |

文档中的数字、端口、接口数量必须与代码一致；`admintem-docs/tests` 下的检查器是提交门禁。

## 10. 已知易错点

1. **Windows PowerShell 写文件编码**：修改含中文的 `vue`/`md` 文件时使用 UTF-8（无 BOM 优先），
   不要用默认编码覆盖，避免中文乱码。
2. **Flyway 与全量 init 双路径**：已发布迁移不可修改；新增列只写在迁移中，不要提前加进 init 的 CREATE TABLE，否则 V1.0.11 这类 ALTER 会重复执行。新增表/权限才需要在 init 中同步，并确保 `IF NOT EXISTS`/`ON DUPLICATE KEY UPDATE`。
3. **端口**：桌面端默认 5175（不是 5173）；E2E/截图/a11y 脚本都依赖该端口。
4. **动态菜单**：新增页面只加 Vue 文件不够，还必须在权限种子中登记菜单（URL/component/权限 key）。
5. **权限变化**：运行 `gen-role-endpoint-matrix.cjs`；不要手改第 9 节生成区。
6. **refreshToken**：E2E 模拟“会话彻底失效”时要同时清理 access 与 refresh，否则会自动续期。
7. **Element Plus**：未注册的组件不会渲染，新增组件必须补注册与样式。
8. **Docker**：`db-init` 会执行全量脚本；不要把它改成在已有库上无条件 DROP。

## 11. 交付流程

1. 先读相关文档和相邻实现，确认权限标识/表结构/测试口径。
2. 小步实现，先编译/单测，再补文档与检查。
3. 提交信息使用中文祈使句，说明“改了什么、为什么、如何验证”。
4. 本地通过后推 Gitee `master`；GitHub 使用服务器部署密钥推 `main`，再触发/观察 CI 与 nightly。
5. 最终交付时附上提交 SHA、CI/nightly 链接、测试结果和未完成项；不要把未验证项写成已完成。
