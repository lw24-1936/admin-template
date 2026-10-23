# admintem-site · 通用官网模版

面向产品/服务官网的 **Vue 3 模版**，一套代码覆盖：落地页、功能页、价格页、关于页、联系页、404。

- **框架**：Vue 3.5 + Vite 5 + Vue Router 4
- **样式**：Tailwind CSS 3 + shadcn 风格 token（组件源码在项目里，可直接改）
- **动效**：anime.js v4（首屏编队、滚动入场、数字滚动）+ CSS（marquee / border beam）
- **图标**：lucide-vue-next
- **不含 React**：shadcn / Magic UI 的**结构与效果**用 Vue 复刻，不引入 React 运行时

## 快速开始

```bash
cd admintem-site
pnpm install
pnpm dev        # http://localhost:5177
pnpm build      # dist/
pnpm preview
```

## 目录结构

```text
admintem-site/
├── index.html                 字体、SEO meta、挂载点
├── tailwind.config.js         token → Tailwind 语义色映射
├── src/
│   ├── main.js                应用入口（注册 v-reveal 指令）
│   ├── App.vue                跳过导航 + 顶栏 + 路由出口 + 页脚
│   ├── router/index.js        路由、滚动行为、每页 title
│   ├── content/site.js        ★ 站点内容单一出处（换品牌只改这里）
│   ├── styles/main.css        ★ 设计令牌 + 玻璃/网格/动效工具类
│   ├── lib/utils.js           cn()（clsx + tailwind-merge）
│   ├── motion/reveal.js       v-reveal 滚动入场（IntersectionObserver + anime）
│   ├── components/
│   │   ├── ui/                Button / Card / Badge / Accordion / Input / Textarea / SectionHeading
│   │   ├── magic/             NumberTicker / BorderBeam / Marquee / DotPattern
│   │   └── site/              Header / Footer / Logo / ProductPreview / PricingCard / CtaSection
│   └── views/                 Home / Features / Pricing / About / Contact / NotFound
└── public/favicon.svg
```

## 二次开发：换成一个新产品的官网

1. **品牌与文案**：改 `src/content/site.js`（名称、导航、hero、功能、价格、FAQ、联系方式）。
2. **配色**：改 `src/styles/main.css` 里的 `--primary / --accent / --background ...`（RGB 三元组），
   `tailwind.config.js` 自动映射成 `bg-primary`、`text-muted-foreground` 等语义类。
3. **Logo**：改 `src/components/site/Logo.vue` 与 `public/favicon.svg`。
4. **页面**：在 `src/views` 新建并注册到 `src/router/index.js`；导航从 `site.nav` 生成。
5. **表单**：`ContactView.vue` 的 `submit()` 目前只做本地校验；替换成你的 API 调用即可。

## 设计系统

| 项 | 值 |
| ---- | ---- |
| 风格 | Glassmorphism（浅色、蓝主色、橙色点缀） |
| 主色 | `#2563EB`（白字对比 5.1:1） |
| 强调 | `#EA580C`（配深色字，避免白字对比不足） |
| 背景 / 卡 | `#F8FAFC` / `#FFFFFF` |
| 正文 | `#0F172A` / `#475569` |
| 字体 | Plus Jakarta Sans（Google Fonts，系统字体兜底） |
| 圆角 | `--radius: 14px`，组件从 token 派生 |

组件只消费语义 token（`bg-card`、`text-muted-foreground`、`border-border`），不写死品牌色；
深色主题的 token 已备好（`html.dark`），默认保持浅色。

## shadcn / Magic UI 在 Vue 里的落地方式

两个库本身是 React 生态，本模版按它们官方"复制源码"的思路，在 Vue 里复刻**契约**而不是依赖：

| 来源 | 本模版实现 |
| ---- | ---- |
| shadcn Button / Card / Badge / Input / Accordion | `components/ui/*`，token 结构、焦点环、`aria-expanded/controls` 对齐 Radix 契约 |
| Magic UI `marquee` | `Marquee.vue`：双份内容 + `translateX(-50%)`，reduced-motion 停 |
| Magic UI `border-beam` | `BorderBeam.vue`：conic-gradient + mask 只留 1px 边框，纯 CSS |
| Magic UI `number-ticker` | `NumberTicker.vue`：进入视口后 anime.js 计数 |
| Magic UI `dot-pattern` | `DotPattern.vue`：CSS 径向渐变 + mask |
| Magic UI `blur-fade` | `v-reveal` 指令：opacity + y + IntersectionObserver |

约束（来自技能规范）：一个区块最多一个"魔法"效果；首屏 LCP 不是 canvas；所有循环动画在
`prefers-reduced-motion` 下停止；渐变文字只做装饰，不承载正文信息。

## anime.js 用法

- 首屏：`HomeView` 里 `createTimeline()` 编队入场，总时长 ≈ 800ms；
- 滚动：`v-reveal` 用 IntersectionObserver 触发，一次性播放；
- 数字：`NumberTicker` 在视口内从 0 数到目标值；
- 只动 `transform / opacity / filter`，不写 `width/height/top/left`；
- 全部动效在 reduced-motion 下短路，内容保持最终态。

## 可访问性 / 性能

- 跳过导航链接、语义化 `header/main/footer/nav`、每页唯一 `h1`；
- 移动菜单：`aria-expanded/controls`、Esc 关闭、锁滚动、焦点进入与归还；
- FAQ：`button + aria-expanded + aria-controls`，面板 `role=region` 关联标题；
- 表单：可见 label、`aria-invalid`、错误就近展示、成功态 `role=status`；
- 焦点环 2px 且不依赖动画；正文对比度 ≥ 4.5:1；
- 无图片 hero（产品预览是 DOM/CSS），避免 LCP 大图；路由级懒加载。

## 验证记录

| 项 | 结果 |
| ---- | ---- |
| `pnpm build` | ✅ 通过 |
| 控制台错误 | ✅ 0（5 个页面） |
| 横向滚动 | ✅ 1440 / 390 均无溢出 |
| 交互 | ✅ FAQ 展开、年付切换（¥54）、表单 3 项校验、提交成功、移动菜单 Esc 关闭 |
| 语义检查 | ✅ 每页 1 个 h1、图片均有 alt、无空链接/空按钮 |

## 已知限制

- 表单只做前端演示，不发送请求；
- 价格、客户 logo、案例均为占位内容，上线前替换；
- "登录"指向 `https://app.example.com` 占位，改成你的应用地址；
- 字体走 Google Fonts CDN，内网部署可改为本地字体文件；
- 未接入 i18n；如需多语言，可在 `content/` 下按语言拆分。
