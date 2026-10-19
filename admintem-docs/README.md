# admintem-work-new —— 权限管理系统重构（Spring Boot 3.5 + Vue 3）

本目录是旧项目 `demo/`（Spring Boot 2.4.2 + Vue 2 + Element UI）的**权限模块全新重构版**，仅保留
**登录 / 注册 / 用户管理 / 角色管理 / 权限管理**能力，并把权限细化到**按钮级**（permission_key）。

## 目录结构

| 目录 | 说明 |
| ---- | ---- |
| `admintem-backend/` | 后端：Spring Boot 3.5.16 + JDK 17 + Spring Security 6 + MyBatis-Plus 3.5 + Redis |
| `admintem-vue/` | 前端：Vue 3.5 + Vite（create-vue 官方脚手架）+ Element Plus + Pinia + Vue Router |
| `admintem-docs/` | 设计 / 数据库 / 接口 / 操作 / 测试文档 与 SQL 变更脚本 |

## 数据约定（重要）

- **数据库**：沿用旧项目 MySQL 库 `admintem_new`（`sys_auth_*` 五张 RBAC 表），只做增量变更；
  增量脚本见 `admintem-docs/db/changelog/`（V1.0.1 已应用）。
- **Redis**：沿用旧项目同一实例（database 0），新增键统一使用 `admintem:auth:*` 前缀，
  与旧项目 `security:jwt:` 等前缀隔离，可并行运行互不干扰。
- **默认账号**（沿用旧数据，密码均未改动）：
  - 管理员：`admin / 12345678`（role_key=admin，超级角色）
  - 测试角色用户：`test / 12345678`（可打开“用户管理/角色管理”页面但没有任何写按钮）

## 快速启动

```bash
# 后端（JDK17）
cd admintem-backend
mvn spring-boot:run          # 默认 dev，端口 8090；接口文档 http://localhost:8090/swagger-ui/index.html

# 前端（Node 18+）
cd admintem-vue
npm install
npm run dev                  # 端口 5173，浏览器打开 http://localhost:5173
```

> 详细步骤见 `admintem-docs/04-部署与操作手册.md`。

## 回归测试

```powershell
cd admintem-docs/tests
pwsh api-regression-test.ps1          # 60 项全功能接口回归（自动清理临时数据）
```

## 文档导航

1. [01-需求与范围](./01-需求与范围.md)
2. [02-系统设计文档](./02-系统设计文档.md)
3. [03-数据库设计与变更说明](./03-数据库设计与变更说明.md)
4. [04-部署与操作手册](./04-部署与操作手册.md)
5. [05-接口与权限标识对照表](./05-接口与权限标识对照表.md)
6. [06-测试报告](./06-测试报告.md)
7. [07-维护与文档同步约定](./07-维护与文档同步约定.md)
8. [08-权限动态认证设计与原项目对比](./08-权限动态认证设计与原项目对比.md)
9. [09-代码审计与质量记录](./09-代码审计与质量记录.md)
10. [10-后续功能扩展规划](./10-后续功能扩展规划.md)
11. [11-优化任务清单与时间窗计划](./11-优化任务清单与时间窗计划.md)
12. [12-页面与界面说明（含全部页面截图）](./12-页面与界面说明.md)
