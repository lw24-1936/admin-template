# admintem-vue · 权限管理桌面端

基于 **Vue 3 + Vite + Element Plus** 的权限管理桌面端，与后端 `admintem-backend` 配合使用，
实现登录注册、用户 / 角色 / 权限管理，以及**菜单级 + 按钮级**的权限控制与数据库驱动的动态路由。

- 开发地址：http://localhost:5175
- 后端接口：默认 http://localhost:8090（dev）
- 代码根目录：`src/`
- 同仓库的移动端（`admintem-mobile`，登录后「首页 / 我的」，端口 5174）复用同一后端与同一套
  `permission_key`，详见 [`../admintem-mobile/README.md`](../admintem-mobile/README.md)。

---

## 一、页面与功能

| 页面 | 路由 | 说明 |
| ---- | ---- | ---- |
| 登录 | `/login` | 账号密码登录、记住我、跳转注册 |
| 注册 | `/register` | 自助注册，注册后自动分配默认角色 |
| 工作台 | `/HomeView` | 登录后的默认首页，展示角色 / 菜单 / 权限概览 |
| 用户管理 | `/userInfo` | 分页查询、新增、编辑、删除、重置密码、分配角色 |
| 角色管理 | `/roleInfo` | 角色 CRUD、树形分配菜单与按钮权限 |
| 权限管理 | `/permissionInfo` | 菜单 / 按钮两级权限维护（新增子级、编辑、级联删除） |
| 无权限 / 404 | `/403`、兜底 | 无权限提示与未知路径兜底 |

页面顶部提供**完整面包屑**（父级 → 子级，如 `系统管理 / 用户管理`），右上角为用户下拉
（个人资料、修改密码、退出登录）。

## 二、技术栈

| 组件 | 版本 / 说明 |
| ---- | ---- |
| Vue | 3.5（`<script setup>` 组合式 API） |
| 构建 | Vite 8（create-vue 官方脚手架） |
| 路由 | Vue Router 5（动态 addRoute） |
| 状态 | Pinia |
| UI | Element Plus 2.x + @element-plus/icons-vue |
| 请求 | Axios（统一拦截器） |

## 三、目录结构

```
admintem-vue
├─ index.html
├─ vite.config.js                 开发端口 5175、路径别名 @
├─ .env.development / .env.test / .env.pre / .env.pro   多环境接口地址
├─ scripts/reorder-sfc.ps1        单文件组件结构顺序规整工具
├─ tests/unit/                    工具函数单测（Node 内置 node --test，零依赖）
└─ src
   ├─ main.js                     入口：注册 Pinia / Router / Element Plus（按需注册）/ v-perm
   ├─ App.vue                     路由出口 + Element Plus 中文语言包（ElConfigProvider）
   ├─ api/                        auth.js、user.js、role.js、permission.js
   ├─ router/index.js             固定路由 + 动态路由注册 + 全局守卫
   ├─ stores/user.js              登录态、用户信息、菜单树、权限集合
   ├─ directives/permission.js    v-perm 按钮权限指令（fail-closed）
   ├─ utils/                      request.js（Axios 封装）、auth.js（令牌存取）、menu.js（菜单展示过滤）
   ├─ layout/                     Layout.vue（框架）、MenuItem.vue（递归菜单）
   ├─ components/                 TableToolbar.vue、PaginationBar.vue 等公共组件
   └─ views/                      login、register、HomeView、userInfo、error
```

> 组件库按需引入：`main.js` 只注册用到的 Element Plus 组件与样式（体积约为全量的 1/2），
> 新增组件时需在 `main.js` 的组件数组与样式引入中各补一行，否则会出现"组件未解析"告警
> （`admintem-docs/tests/e2e-admin-login.cjs` 已包含该断言）。

## 四、快速开始

```powershell
cd admintem-vue
npm install
npm run dev            # http://localhost:5175
```

构建产物输出到 `dist/`：

```powershell
npm run build          # 生产（.env.production）
npm run build:test     # 测试（.env.test，接口指向 8091）
npm run build:pre      # 预发布（.env.pre，接口指向 8092）
npm run build:pro      # 生产档（.env.pro，接口指向 8093）
npm run test           # 工具函数单测（Node 内置 node --test，无需额外依赖）
npm run preview        # 本地预览构建产物
```

后端地址通过环境文件 `VITE_API_BASE_URL` 配置，例如 `.env.development`：

```
VITE_API_BASE_URL=http://localhost:8090
```

## 五、权限控制

### 5.1 会话数据

登录成功后后端返回并保存在 Pinia 中：

- `permissions`：全部权限标识集合（菜单 + 按钮的 `permission_key`）；
- `menus`：该用户可访问的菜单树（含 `menuUrl`、`component`、`permissionKey`）；
- `roles`：用户角色列表。

### 5.2 按钮级权限（v-perm）

无权限时元素直接从 DOM 移除，避免“看得见点不动”：

```vue
<el-button v-perm="'sysAuthUser:addSysAuthUser'" type="primary" @click="openAdd">
  新增用户
</el-button>

<!-- 数组表示满足其一即可 -->
<el-button v-perm="['sysAuthUser:editSysAuthUser', 'sysAuthUser:deleteById']">编辑</el-button>
```

采用 **fail-closed** 语义（与移动端一致）：以下三种情况都不渲染受控元素——

| 情况 | 行为 |
| ---- | ---- |
| 会话未加载完成（`permissions` 为空） | 不渲染，避免“先显示后隐藏”的瞬间暴露 |
| 指令值漏写（`v-perm=""` / `undefined`） | 不渲染，并在控制台报错（属配置遗漏） |
| 用户不具备任一标识 | 不渲染 |

`stores/user.js` 的 `hasPerm(key)` 同样对空标识返回 `false`，不存在“默认放行”路径。
前端隐藏仅是体验，安全边界仍在后端 `@PreAuthorize`。

### 5.3 页面级权限与动态路由

- 固定路由仅 `/login`、`/register`、`/403` 与根布局；
- 业务页面由登录后下发的菜单动态注册：`component` 字段为 `src/views` 下相对路径
  （如 `userInfo/UserInfo`），前端用 `import.meta.glob` 建立映射并 `router.addRoute` 挂载；
- 路由守卫按 `meta.perm` 兜底校验，未知/无权限路径统一进入 404 提示页；
- **新增页面三要素**：① 在 `src/views` 放置组件；② 在「权限管理」登记菜单
  （菜单路由 + 组件路径 + 权限标识）；③ 在「角色管理」授权给角色。无需修改路由表。
- **菜单可见性**：`status=1`（禁用）的菜单后端不下发，页面不可访问；`show=1`（隐藏）的菜单
  仍会下发并注册路由（可 URL 直达），仅在左导航与菜单清单中不显示
  （过滤逻辑见 `src/utils/menu.js`，语义见《03-数据库设计与变更说明》V1.0.3）。

### 5.4 请求令牌

所有请求自动携带请求头：

```
admintem_token: LAUNCH <jwt>
```

收到 HTTP 401 时拦截器会清理本地令牌并跳转登录页；业务 `code != 200` 统一弹出错误信息。

## 六、公共组件与代码规范

| 组件 / 规范 | 说明 |
| ---- | ---- |
| `components/TableToolbar.vue` | 列表页工具条：默认插槽放筛选控件，`#actions` 插槽放按钮 |
| `components/PaginationBar.vue` | 分页条：`total / page / size` 受控，`@change` 回传页码与条数 |
| `layout/MenuItem.vue` | 递归菜单项，自动区分布局分组与叶子菜单 |
| 单文件组件顺序 | 统一 `<template>` → `<script setup>` → `<style>`（可用 `scripts/reorder-sfc.ps1` 校验/整理） |
| 页面结构 | 列表页统一为「工具条 + 表格 + 分页 + 弹窗」，弹窗内保存/取消按钮样式一致 |

## 七、部署

构建后将 `dist/` 交给 Nginx 托管，并把后端接口反代到同一域名：

```nginx
server {
    listen 80;
    root /path/to/admintem-vue/dist;
    location / { try_files $uri $uri/ /index.html; }   # 前端使用 HTML5 history 路由
    location ~ ^/(auth|sysAuthUser|sysAuthRole|sysAuthPermission) {
        proxy_pass http://127.0.0.1:8090;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

同域部署时可将环境文件中的 `VITE_API_BASE_URL` 置空，直接使用相对路径请求。

## 八、常见问题

| 现象 | 处理 |
| ---- | ---- |
| 登录后提示登录过期 | 后端未启动或令牌被吊销；确认 8090 可访问并重新登录 |
| 按钮不显示 | 当前账号缺少该按钮的 `permission_key`，属预期行为（后端同样会返回 403） |
| 页面 404 | 菜单未授权或 `component` 路径与 `src/views` 下文件不匹配 |
| 面包屑只显示单级 | 该页面在菜单树中为顶级菜单，或菜单数据未刷新（重新登录即可） |
| 接口跨域 | 开发环境后端已放开 `localhost:*`；生产建议同域反代 |

## 九、文档索引

完整设计与操作文档位于 `../admintem-docs/`：需求范围、系统设计、数据库说明、部署操作手册、
接口与权限标识对照表、测试报告、维护与文档同步约定、动态认证说明、代码审计记录、
后续功能扩展规划。
