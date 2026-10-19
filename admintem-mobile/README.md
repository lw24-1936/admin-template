# admintem-mobile · 权限管理系统移动端

移动端是 admintem 权限管理系统的一个**端**，与 `admintem-backend`、`admintem-vue` 同属一个整体：
共用同一套后端接口、同一套 `permission_key` 权限标识、同一套令牌约定。

当前为**基础版本**：登录后提供底部标签栏两个页面 —— **首页** 与 **我的**。

---

## 一、技术栈与选型

| 项 | 选择 | 说明 |
| ---- | ---- | ---- |
| 框架 | Vue 3.5 + Vite 8 | 与桌面端同一代框架，构建链一致 |
| 路由 | Vue Router 5（**hash 模式**） | 静态托管或手机直接打开时无需服务端 rewrite 配置 |
| 状态 | Pinia | 与桌面端同结构的会话状态（用户/角色/菜单/权限） |
| UI | Vant 4（**按需引入**） | 移动端组件库；各页面只引入用到的组件与样式，产物体积显著小于全量引入 |

> 体积优化：`main.js` 不再全量注册 Vant；页面通过 `import { Button as VanButton } from 'vant'`
> 与 `import 'vant/es/button/style/index'` 按需加载（模板仍写 `<van-button>`）。
> 实测 CSS 203.6 KB → 87.6 KB（-57%），JS 383.4 KB → 206.0 KB（-46%）。
| 请求 | Axios | 复用桌面端约定：请求头 `admintem_token: "LAUNCH <token>"`、返回体 `{ code, message, data }` |
| 端口 | dev 5174 | 与桌面端 5173 区分，可同时运行；后端仍为 8090 |

## 二、功能范围（基础版）

| 页面 | 路由 | 功能 |
| ---- | ---- | ---- |
| 登录 | `#/login` | 账号密码登录、记住我（7 天免登录）、登录后回跳来源页 |
| 首页 | `#/home` | 欢迎信息（昵称/角色）、数据概览（菜单/权限/角色数量）、按权限渲染的管理入口、可访问菜单清单 |
| 我的 | `#/mine` | 个人资料与角色标签、账号信息（账号/昵称/邮箱/手机号/状态）、修改密码、关于、退出登录 |

已实现的基础能力：

- **登录态**：令牌写入 `localStorage`（key `admintem_token`），刷新页面自动恢复会话（`/auth/getUserInfo`）；
- **权限渲染**：`v-perm` 指令按 `permission_key` 决定管理入口是否渲染，无权限的元素直接从 DOM 移除；
  采用 **fail-closed** 语义——会话未加载完成、指令值漏写（控制台报错）、无权限三种情况均不渲染，
  与桌面端行为一致；
- **修改密码**：调用 `/auth/updatePassword`，成功后服务端吊销令牌并要求重新登录；
- **退出登录**：二次确认 → 调用 `/auth/logout` → 清理本地令牌 → 回到登录页；
- **登录态失效**：接口返回 401 时统一清理令牌并跳回登录页（带 `redirect` 回跳参数）。

> 移动端不承载完整的用户/角色/权限管理界面：点管理入口会明确提示「请在桌面端使用」。
> 后续扩展方向见《10-后续功能扩展规划》。

## 三、目录结构

```
admintem-mobile
├─ index.html                     手机 viewport 配置、内联 SVG 图标
├─ vite.config.js                 端口 5174、@ 别名、局域网访问
├─ jsconfig.json                  @ 别名在 IDE 中的路径提示
├─ tests/unit/                    工具函数单测（Node 内置 node --test，零依赖）
├─ .env.development               VITE_API_BASE_URL=http://localhost:8090
├─ .env.{test,pre,pro}            测试 / 预发布 / 生产环境后端地址（与桌面端同套约定）
├─ .env.production                生产默认同源（Nginx 反代）
└─ src
   ├─ main.js                     注册 Pinia / Router / Vant / v-perm 指令
   ├─ App.vue                     根组件（路由出口）
   ├─ api/auth.js                 登录、会话、改密、登出接口
   ├─ router/index.js             路由表 + 登录态守卫（hash 模式）
   ├─ stores/user.js              会话状态（用户/角色/菜单/权限）
   ├─ directives/permission.js    按钮级权限指令 v-perm
   ├─ utils/{request,auth}.js     Axios 封装、令牌存取
   ├─ utils/menu.js               菜单展示过滤（isHiddenMenu / flattenVisibleLeafMenus，有单测）
   ├─ views/login/index.vue       登录页（纸面极简风格）
   ├─ views/layout/TabLayout.vue  主布局 + 底部标签栏（首页/我的）
   ├─ views/home/index.vue        首页
   └─ views/mine/index.vue        我的（个人中心）
```

## 四、运行方式

前置：后端已在 8090 运行（见仓库根 README「快速开始」）。

```powershell
cd admintem-mobile
npm install
npm run dev            # http://localhost:5174（使用 .env.development → 后端 8090）
npm run build          # 生产构建，产物 dist/
npm run build:test     # 测试环境构建（.env.test → 后端 8091）
npm run build:pre      # 预发布构建（.env.pre → 后端 8092）
npm run build:pro      # 生产构建（.env.pro / .env.production → 默认同源）
npm run test           # 工具函数单测（Node 内置 node --test，无需额外依赖）
npm run preview        # 本地预览构建产物
```

真机联调：dev server 以 `host: 0.0.0.0` 启动，手机与电脑同一局域网时，
用启动日志中打印的 Network 地址（如 `http://192.168.x.x:5174`）访问；
若后端不在本机，请把 `.env.development` 的 `VITE_API_BASE_URL` 改为可访问的后端地址，
并确认后端 CORS 允许该来源（后端 `admintem.security.cors` 配置）。

演示账号：`admin / 12345678`（管理员）、`test / 12345678`（受限账号，管理入口明显更少）。

## 五、权限在两个端的统一

移动端与桌面端使用**同一份权限标识**，后端注解、桌面端按钮、移动端入口三者一一对应：

| 位置 | 写法 |
| ---- | ---- |
| 数据库 | `sys_auth_permission.permission_key`（`type=0` 菜单 / `type=1` 按钮） |
| 后端 | `@PreAuthorize("@rbac.hasPerm('userInfo:userInfo')")` |
| 桌面端 | `<el-button v-perm="'sysAuthUser:addSysAuthUser'">新增用户</el-button>` |
| 移动端 | `<button v-perm="'sysAuthUser:addSysAuthUser'">新增用户</button>` |

移动端首页的「管理入口」即取自菜单权限标识（`userInfo:userInfo`、`userInfo:roleInfo`、
`userInfo:permissionInfo`）与一个按钮级标识（`sysAuthUser:addSysAuthUser`）作为示例：
`admin` 可见 4 项，`test` 只能看到其被授权的 2 项，未授权的入口不渲染。

## 六、验证

移动端使用真实浏览器（Edge headless、390×844 手机视口、开启触摸）做端到端验证：

```powershell
# 前置：后端 8090 已启动、admintem-mobile 的 npm run dev 已启动（5174）
cd admintem-docs/tests
node e2e-mobile-login.cjs
```

脚本覆盖 41 项断言：登录页渲染与无横向溢出、真实表单登录跳转、底部标签栏两项、
首页会话数据与按权限渲染的管理入口、我的页信息与退出登录、`test` 账号权限收敛、
标签栏 fixed 定位与几何尺寸、内容不被遮挡、触摸区域高度、控制台零错误、
令牌失效后自动清理并停留登录页（无重定向回环），以及**修改密码全流程**
（临时账号：改密后强制回登录页 → 旧密码失效 → 新密码可登录 → 账号清理）。
详细结果见《06-测试报告》。

## 七、已知边界

- 仅实现基础登录与两个标签页，管理类操作仍在桌面端完成；
- 令牌与桌面端同构，移动端登录会与桌面端共享登录失败锁定计数、并各自持有独立令牌；
- 未接入消息推送、扫码、离线缓存等能力（见《10-后续功能扩展规划》）。
