# admintem-uniapp · uni-app 客户端

与 `admintem-vue`（桌面端）、`admintem-mobile`（Vant 移动端）**共用同一套后端** `admintem-backend`。
一套接口、一套鉴权、一套权限标识；这个工程只新增"载体"，不改后端契约。

- 技术栈：uni-app（Vue 3）+ Vite 5 + Vue 3.4，**无 UI 组件库**（样式全部自绘，保证三端一致）
- 运行形态：H5（浏览器）/ 微信小程序（同一份源码，条件编译处理差异）
- 设计语言：与桌面端 V2 同一套色板（`#f6f7fa / #1e3a6e / slate 墨阶`），尺寸单位换成 rpx

## 移动端组件库约定

当前页面仍是自绘样式（保证 H5 / 小程序三端一致）；后续新增页面按
《[13-移动端组件库使用约定](../admintem-docs/13-移动端组件库使用约定.md)》接入
**uview-plus**（uView 1.x 的 Vue3 版）：

- Vue3 的 uni-app 不能直接安装 Vue2 版 `uview-ui`，必须用 `uview-plus`；
- 通过 easycom 按需引入（`^u-(.*)` → `uview-plus/components/u-$1/u-$1.vue`）；
- 主题色继续映射到现有 `--lc-*` 色板，不直接使用库默认蓝。

## 1. 快速开始

```bash
cd admintem-uniapp
pnpm install          # 或 npm install

# H5（5176，/api 已反代到 8090）
pnpm dev:h5

# 微信小程序（产物 dist/dev/mp-weixin，用开发者工具导入）
pnpm dev:mp-weixin

# 生产构建
pnpm build:h5
pnpm build:mp-weixin
```

| 入口 | 地址 |
| ---- | ---- |
| H5 | http://localhost:5176 |
| 后端 | http://localhost:8090（需先启动，见根 README） |

演示账号：`admin / 12345678`（管理员）、`test / 12345678`（普通账号）。

## 2. 与后端的契约（与桌面端完全一致）

| 项 | 约定 |
| ---- | ---- |
| 令牌请求头 | `admintem_token: LAUNCH <jwt>` |
| 本地存储键 | `admintem_token`（令牌）、`admintem_user`（会话缓存）、`admintem_remember_username` |
| 响应体 | `{ code, message, data }`，`code !== 200` 视为业务失败 |
| 401 | 清空登录态并回到登录页（并发 401 只跳一次） |

用到的接口：

| 方法 | 路径 | 用途 |
| ---- | ---- | ---- |
| POST | `/auth/login` | 登录，返回 `{ token, user }` |
| POST | `/auth/register` | 注册（受后端 `admintem.register-enabled` 控制） |
| POST | `/auth/logout` | 退出（服务端令牌白名单立即失效） |
| POST | `/auth/getUserInfo` | 拉取会话：用户 + 角色 + 菜单 + 权限标识 |
| POST | `/auth/updateProfile` | 修改本人资料（昵称/邮箱/手机/性别/介绍/头像） |
| POST | `/auth/updatePassword` | 修改本人密码（成功后需重新登录） |
| POST | `/auth/uploadAvatar` | 上传头像（multipart，字段名 `file`） |

## 3. 页面

| 页面 | 路径 | 说明 |
| ---- | ---- | ---- |
| 登录 | `pages/login/index` | 账号/密码/记住账号；已登录直接进工作台 |
| 注册 | `pages/register/index` | 校验规则与后端 `RegisterRequest` 对齐 |
| 工作台 | `pages/home/index` | 问候 + 身份 + 指标（菜单/权限/角色）+ 快捷入口 + 我的菜单 |
| 我的 | `pages/mine/index` | 资料头、账号概览、账号设置、登录信息、退出 |
| 个人资料 | `pages/profile/index` | 头像上传、昵称/邮箱/手机/性别/介绍 |
| 修改密码 | `pages/password/index` | 校验后调用后端，成功清本地并回登录页 |

tabBar 只有「工作台 / 我的」两页；个人资料与修改密码是普通页面（可返回）。

## 4. 目录结构

```text
admintem-uniapp/
├── src/
│   ├── App.vue                 全局样式（设计令牌与通用类）
│   ├── main.js                 uni-app 入口
│   ├── pages.json              页面与 tabBar 配置
│   ├── manifest.json           应用信息（H5 / 微信小程序）
│   ├── styles/tokens.css       设计令牌（色板/圆角/投影）
│   ├── config/index.js         后端地址与存储键（H5 走 /api 代理）
│   ├── utils/
│   │   ├── request.js          uni.request 封装（鉴权头、401、统一错误）
│   │   ├── storage.js          令牌/会话缓存
│   │   └── media.js            头像等相对路径解析
│   ├── api/auth.js             认证接口
│   ├── stores/user.js          会话状态（reactive 单例，未引入 Pinia）
│   ├── pages/                  login / register / home / mine / profile / password
│   └── static/                 logo 与 tabBar 图标（由品牌 SVG 生成）
├── vite.config.js              5176 端口 + /api、/uploads 反代
└── index.html                  H5 模板（favicon、标题）
```

## 5. 三端差异与注意点

| 项 | H5 | 微信小程序 |
| ---- | ---- | ---- |
| 后端地址 | Vite 代理 `/api` → `http://localhost:8090` | `http://localhost:8090`（绝对地址） |
| 跨域 | 由代理规避 | 需在开发者工具勾选「不校验合法域名」；真机改成局域网 IP |
| 令牌存储 | `uni.setStorageSync`（落到 localStorage） | `uni.setStorageSync`（小程序 Storage） |
| 头像上传 | `<input type=file>`（uni.chooseImage 降级） | 相册/相机 |
| 部署 | 静态资源 + Nginx 反代 `/api`、`/uploads` | 上传后在小程序后台配置 request 合法域名 |

> 小程序端定位是"随身查看 + 自助维护"：工作台展示会话与菜单，个人资料/改密可操作；
> 用户/角色/权限/审计/部门等列表维护页面仍由桌面端承载（菜单上标注「桌面端」）。

## 6. 设计实现要点

- **令牌单一出处**：`styles/tokens.css` 定义 `--c-*`（颜色）、`--r-*`（圆角）、`--shadow-*`、`--grad-brand`；
  页面里不写裸色值，改主题只改这一个文件。
- **尺寸用 rpx**：750 设计宽度等比缩放；触摸目标（输入框/按钮）统一 92rpx ≈ 46px，满足 ≥44px 的可点击标准。
- **渐变只做装饰**：品牌渐变用于英雄卡与主按钮，文字仍用纯色 token，保证对比度。
- **动效克制**：只用 `transition` 做按压反馈；不引入动画库（小程序包体与性能优先）。
- **空态与错误**：请求失败由 `utils/request.js` 统一 toast；401 统一回登录；页面保留会话缓存避免白屏。

## 7. 验证记录

| 项 | 方法 | 结果 |
| ---- | ---- | ---- |
| H5 编译 | `pnpm dev:h5` / `pnpm build:h5` | ✅ 通过（24 个产物文件） |
| 微信小程序编译 | `pnpm build:mp-weixin` | ✅ 通过（app.json 含 6 页 + tabBar，图标随包） |
| 登录链路 | Playwright：admin/12345678 → 工作台 | ✅ `/api/auth/login` 200，跳转 `#/pages/home/index` |
| 工作台数据 | 断言指标与菜单列表 | ✅ 7 菜单 / 26 权限 / 2 角色，菜单 7 条 |
| 我的 / 资料 / 改密 | 逐页跳转与退出确认弹窗 | ✅ 页面渲染、弹窗可见、0 控制台错误 |
| 头像上传 | `uni.chooseImage` + `/auth/uploadAvatar` | 代码路径就绪；自动化未模拟文件选择 |

## 8. 打包成 App

可以。uni-app 的 App 端与 H5/小程序共用同一份页面代码，本工程已经具备构建条件：

```bash
pnpm build:app     # 产物：dist/build/app（App 资源包，HBuilderX 可直接导入运行）
```

### 8.1 产物与验证

| 项 | 结果 |
| ---- | ---- |
| App 资源包构建 | ✅ DONE（app-service.js / app.css / manifest.json / view 容器） |
| 平台声明 | android / iPhone / iPad |
| 模块 | Camera、Gallery（头像拍摄/相册） |
| Android 权限 | INTERNET、ACCESS_NETWORK_STATE、CAMERA、READ/WRITE_EXTERNAL_STORAGE |
| 屏幕方向 | portrait-primary |
| App 图标 | `src/static/app/icon-1024.png`（由品牌 SVG 生成） |

> 本次只验证到"资源包"这一步：本机没有 HBuilderX、Android Studio 与签名证书，
> 因此**没有生成 APK / IPA**。下面两条路线任选其一即可出安装包。

### 8.2 路线 A：HBuilderX 云打包（推荐，最省事）

1. 安装 HBuilderX，用 DCloud 账号登录；
2. 打开 `src/manifest.json` → 「基础配置」里点「重新获取」得到 **AppID**（形如 `__UNI__XXXXXXX`），
   填到 `manifest.json` 的 `appid` 字段（当前为空，空 appid 无法云打包）；
3. 「App 图标配置」选择 `src/static/app/icon-1024.png`，一键生成各尺寸；
4. 菜单「发行 → 原生 App-云打包」：
   - Android：包名（如 `com.admintem.app`）+ 证书（可用 HBuilderX 自动生成）
   - iOS：Apple 开发者证书 + Provisioning Profile（没有证书时只能打自定义基座）
5. 云打包完成后下载 APK / IPA。

### 8.3 路线 B：离线打包（要自己控壳与签名）

1. `pnpm build:app` 得到 `dist/build/app`；
2. 下载 DCloud **App 离线 SDK**（Android/iOS），在其示例工程里替换资源目录；
3. 配置离线打包 AppKey（DCloud 开发者中心申请）、包名、签名与图标；
4. 用 Android Studio / Xcode 出包。

这条路线适合要集成原生 SDK（推送、支付、地图）或走企业签名流程的场景。

### 8.4 真机必改的两件事

**① 后端地址**：默认 `http://localhost:8090` 在手机上是"手机自己"，必须换成局域网 IP 或域名：

```bash
# Windows 上查本机内网 IP：ipconfig → IPv4 地址
VITE_API_BASE_URL=http://192.168.1.214:8090 pnpm build:app
```

> 这个变量对 H5 / 小程序 / App 都生效（`vite.config.js` 用 `define` 注入，
> 因为 uni-app 的 app 构建不会把 `VITE_*` 注入 `import.meta.env`）。
> 已实测：注入后 `app-service.js` 中出现该地址，默认构建则回落到 `localhost:8090`。

**② 明文 HTTP**：Android 9+ 与 iOS ATS 默认限制明文请求，正式包建议用 HTTPS 域名；
若只能内网 HTTP，需要在打包配置里放开（离线打包改 `networkSecurityConfig` / ATS 例外）。

### 8.5 打包相关的已知限制

- 未生成 APK/IPA：缺 HBuilderX、Android SDK 与签名证书；
- `appid` 为空，云打包前必须换成自己的 DCloud AppID；
- App 端同样不做列表维护页（与桌面端分工）。

## 9. 已知限制

- 微信小程序的 `appid` 为空，需替换为自己的小程序 AppID 才能真机预览；
- 生产 H5 若与后端不同源，需要 Nginx 反代 `/api` 与 `/uploads`，或构建时设置 `VITE_API_BASE_URL`；
- 注册开关由后端控制，关闭时注册页会收到业务错误提示；
- 小程序端不做列表维护类页面（与桌面端分工），如需可在此基础上扩展。
