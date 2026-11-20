# admintem 文档中心

> 本目录是 `admintem` 的设计、数据库、接口、部署与验收文档。
> 根目录 [README](../README.md) 负责项目介绍与快速开始；这里负责可追溯的设计与运维细节。

## 快速导航

| 文档 | 适合谁看 | 内容 |
| ---- | ---- | ---- |
| [01-需求与范围](./01-需求与范围.md) | 产品 / 开发 | 目标、边界、角色与验收口径 |
| [02-系统设计文档](./02-系统设计文档.md) | 开发 / 架构 | 鉴权链路、缓存、审计、上传、部门 |
| [03-数据库设计与变更说明](./03-数据库设计与变更说明.md) | 开发 / DBA | 表结构、V1.0.1~V1.0.14 变更与回滚 |
| [04-部署与操作手册](./04-部署与操作手册.md) | 运维 / 开发 | 环境、配置、启动、故障处理 |
| [05-接口与权限标识对照表](./05-接口与权限标识对照表.md) | 开发 / 测试 | 接口与 `permission_key` 矩阵 |
| [06-测试报告](./06-测试报告.md) | 测试 / 评审 | 各轮实测数据与验收结论 |
| [07-维护与文档同步约定](./07-维护与文档同步约定.md) | 全体 | 改代码必须同步的文档与脚本 |
| [08-权限动态认证设计与原项目对比](./08-权限动态认证设计与原项目对比.md) | 架构 / 评审 | 与旧项目实现的差异与取舍 |
| [09-代码审计与质量记录](./09-代码审计与质量记录.md) | 开发 / 评审 | 优化动机、踩坑与事故复盘 |
| [10-后续功能扩展规划](./10-后续功能扩展规划.md) | 产品 / 开发 | 可继续做的方向 |
| [11-优化任务清单与时间窗计划](./11-优化任务清单与时间窗计划.md) | 项目管理 | 批次记录与验收口径 |
| [12-页面与界面说明](./12-页面与界面说明.md) | 前端 / 测试 | 页面字段、按钮、权限与截图 |
| [13-移动端组件库使用约定](./13-移动端组件库使用约定.md) | 移动端开发 | Vant 组件与页面模式 |
| [14-文件存储策略与 MinIO 接入](./14-文件存储策略与MinIO接入.md) | 后端 / 运维 | 本地/MinIO 策略与切换 |
| [15-后端方法日志规范](./15-后端方法日志规范.md) | 后端 / 运维 | 方法日志字段、脱敏与检索 |
| [16-Jenkins 自由风格部署教程](./16-Jenkins自由风格部署教程.md) | 运维 | Jenkins 部署全流程 |

## 当前功能边界

已实现：登录/注册/退出/改密、refreshToken 轮换、个人资料与头像、用户管理、角色管理、权限管理（菜单 + 按钮）、
行级数据权限（全部/本部门/本部门及子部门/本人）、部门管理、操作审计、在线会话、系统参数、数据字典、文件中心、
本地/MinIO 头像存储、桌面端深色主题、请求 `traceId`、健康检查与 Prometheus 指标。

仍待扩展：审计变更 diff、审计告警、i18n、SSO/MFA、代码生成器与 API 版本化。
这些在 [10-后续功能扩展规划](./10-后续功能扩展规划.md) 中单独立项。

## 本地环境

| 服务 | 地址 / 端口 | 说明 |
| ---- | ---- | ---- |
| 后端 | `http://localhost:8090` | dev profile，健康检查 `/actuator/health` |
| Swagger | `http://localhost:8090/swagger-ui/index.html` | Authorize 填 `LAUNCH <token>` |
| 桌面端 | `http://localhost:5175` | Vue 3 + Element Plus |
| 移动端 | `http://localhost:5174/#/login` | Vue 3 + Vant 4 |
| uni-app H5 | `http://localhost:5176` | 可选 |
| 官网模版 | `http://localhost:5177` | 可选 |

默认账号：`admin / 12345678`（管理员）、`test / 12345678`（受限账号）。
**首次部署后必须修改默认密码。**

## 数据库脚本

```text
db/init/00_admintem_full_init.sql   全量初始化（建库后执行，内含显式确认闸门）
db/changelog/V1.0.1 ~ V1.0.14       Flyway 增量迁移（只新增，不修改历史）
```

初始化脚本会 DROP 并重建 `sys_auth_*`、`sys_dept`、`sys_auth_audit_log`。
执行前必须 `SET @admintem_confirm_reset = 1`，否则 `SIGNAL` 中止。

## 测试与验收

| 层级 | 命令 | 当前结果 |
| ---- | ---- | ---- |
| 后端单测 | `cd admintem-backend && mvn test` | **162 / 162** |
| 接口回归 | `pwsh -NoProfile -File admintem-docs/tests/api-regression-test.ps1` | **78 / 78** |
| 桌面 E2E | `node admintem-docs/tests/e2e-admin-login.cjs` | **21 / 21** |
| 移动 E2E | `node admintem-docs/tests/e2e-mobile-login.cjs` | **44 / 44** |
| 设计层 a11y | `node admintem-docs/tests/check-design-a11y.cjs` | **123 / 123** |
| 前端单测 | `cd admintem-vue && npm test`；`cd admintem-mobile && npm test` | **10 + 6** |
| 文档/权限检查 | `check-perm-keys`、`check-permission-doc-consistency`、`check-doc-images`、`check-tokens`、矩阵 `--check` | 全通过 |

接口回归与 E2E 需要真实 MySQL/Redis/后端/dev server；CI 的 `ci.yml` 跑不需要中间件的部分，
`e2e-nightly.yml` 跑完整回归，详见根 README 的「质量门禁」。

## 文档维护约定

1. 改接口、权限标识、数据库字段或页面行为时，同步更新对应文档与 [05](./05-接口与权限标识对照表.md)。
2. 改前端权限标识后执行 `node admintem-docs/tests/check-perm-keys.cjs`。
3. 改页面并更新截图后执行 `node admintem-docs/tests/check-doc-images.cjs`。
4. 新增数据库变更只允许新增 `V1.0.x__xxx.sql`，必须幂等并写明回滚方式。
5. 修改测试基线时，以实际命令输出为准，不手写估值。
