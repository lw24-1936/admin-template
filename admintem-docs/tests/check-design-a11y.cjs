/*
 * E8 可访问性与适配核验（程序化门槛，只读、不写数据）
 *
 * 为什么要有这个脚本：改版里"好不好看"由人评审，但"能不能用"必须是硬指标——
 * 对比度不足、窄屏横向溢出、触摸目标过小这三类问题，肉眼评审几乎发现不了，
 * 却直接影响可用性与无障碍。因此把它们变成可重复执行的断言。
 *
 * 三组检查：
 *   ① 对比度（纯计算，不依赖浏览器）：按 WCAG 2.1 相对亮度公式计算 token 配对的对比度；
 *      - 正文级（body）要求 ≥ 4.5:1；
 *      - 次要信息（secondary）要求 ≥ 3:1（大字号/辅助文字的下限）；
 *      - decorative 仅登记数值、不断言（用于纯装饰，如纸面网格线）。
 *   ② 桌面端三档宽度（1280 / 1440 / 1920）：页面级无横向滚动，
 *      且不存在"未被滚动容器裁剪却超出视口"的元素（排除表格自身的横向滚动）。
 *   ③ 移动端三档宽度（360 / 390 / 430）：同样无横向溢出，且触摸目标 ≥ 44px
 *      （底部标签项、首页管理入口、登录按钮）。
 *
 * 运行前提（与两端 E2E 相同）：后端 8090、桌面端 dev 5175、移动端 dev 5174 已启动。
 * 运行：node check-design-a11y.cjs    退出码 0 = 全部通过
 */
const fs = require('fs')
const path = require('path')

/** 解析 playwright-core：本地 -> 全局 playwright 内置副本 -> 全局 playwright */
function loadChromium() {
  const candidates = [
    'playwright-core',
    'C:\\dev\\nodejs\\node_modules\\playwright\\node_modules\\playwright-core',
    'playwright',
  ]
  for (const c of candidates) {
    try {
      const mod = require(c)
      if (mod && mod.chromium) return mod.chromium
    } catch (e) {
      // 继续尝试下一个候选路径
    }
  }
  throw new Error('未找到 playwright-core，请执行 npm i playwright-core 后重试')
}

const chromium = loadChromium()
const EDGE = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe'
// 基址可覆盖：本机 5175 可能被其他项目的 dev server 占用（Windows 允许同端口双绑定）
const DESKTOP = process.env.DESKTOP_BASE_URL || 'http://localhost:5175'
const MOBILE = process.env.MOBILE_BASE_URL || 'http://localhost:5174'
const ROOT = path.resolve(__dirname, '..', '..')

let failed = 0
let passed = 0
function check(label, ok, extra) {
  if (ok) {
    passed++
    console.log('  PASS ' + label + (extra ? ' -> ' + extra : ''))
  } else {
    failed++
    console.log('  FAIL ' + label + (extra ? ' -> ' + extra : ''))
  }
}

/* ==================== ① 对比度 ==================== */

/**
 * 解析 tokens.css 中的 `--lc-*: 值;` 声明。
 * 只处理 hex 与 rgba 两种取值（本项目的 token 只有这两种）。
 */
function parseTokens(file) {
  const css = fs.readFileSync(file, 'utf8')
  const map = {}
  const re = /--lc-([a-z0-9-]+)\s*:\s*([^;]+);/g
  let m
  while ((m = re.exec(css)) !== null) {
    const name = '--lc-' + m[1]
    if (!(name in map)) map[name] = m[2].trim() // 历史别名重复定义时以第一处为准
  }
  return map
}

function hexToRgb(v) {
  const h = v.replace('#', '').trim()
  const full = h.length === 3 ? h.split('').map((c) => c + c).join('') : h
  return [parseInt(full.slice(0, 2), 16), parseInt(full.slice(2, 4), 16), parseInt(full.slice(4, 6), 16)]
}

function rgbaToRgb(v) {
  const m = v.match(/rgba?\(([^)]+)\)/)
  const parts = m[1].split(',').map((s) => parseFloat(s.trim()))
  return [parts[0], parts[1], parts[2], parts.length > 3 ? parts[3] : 1]
}

/** 把可能带 alpha 的颜色合成到不透明底色上（纸面/白卡上叠 rgba 的实际观感） */
function flatten(color, backdrop) {
  if (color.startsWith('#')) return hexToRgb(color)
  const [r, g, b, a] = rgbaToRgb(color)
  const [br, bg, bb] = backdrop.startsWith('#') ? hexToRgb(backdrop) : rgbaToRgb(backdrop)
  return [r * a + br * (1 - a), g * a + bg * (1 - a), b * a + bb * (1 - a)]
}

/** WCAG 2.1 相对亮度 */
function luminance([r, g, b]) {
  const f = (c) => {
    const s = c / 255
    return s <= 0.03928 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4)
  }
  return 0.2126 * f(r) + 0.7152 * f(g) + 0.0722 * f(b)
}

/** WCAG 对比度：(L1+0.05)/(L2+0.05)，与前后景顺序无关 */
function contrast(fg, bg) {
  const l1 = luminance(fg)
  const l2 = luminance(bg)
  const [hi, lo] = l1 > l2 ? [l1, l2] : [l2, l1]
  return (hi + 0.05) / (lo + 0.05)
}

/*
 * token 配对表：fg 为文字色 token（或字面量 #ffffff），bg 为背景 token（或字面量）。
 * profile 说明：
 *   body       正文/表格/按钮文字，门槛 4.5
 *   secondary  次要信息、提示、kicker，门槛 3.0
 *   exempt     WCAG 1.4.3 明确豁免的场景（禁用/inactive 组件），仅登记并写明原因
 *   decorative 纯装饰（网格线/极浅底），仅登记
 * backdrop 用于 rgba 文字的合成底色（本项目文字 token 均为不透明，保留以备用）。
 */
const PAIRS = [
  // —— 墨色阶梯在纸面与白卡上 ——
  { fg: '--lc-ink', bg: '--lc-paper-card', profile: 'body', where: '标题/强调（白卡）' },
  { fg: '--lc-ink', bg: '--lc-paper', profile: 'body', where: '标题/强调（纸面）' },
  { fg: '--lc-ink-body', bg: '--lc-paper-card', profile: 'body', where: '正文（白卡）' },
  { fg: '--lc-ink-body', bg: '--lc-paper', profile: 'body', where: '正文（纸面）' },
  { fg: '--lc-ink-body', bg: '--lc-surface-soft', profile: 'body', where: '正文（次级面）' },
  { fg: '--lc-ink-soft', bg: '--lc-paper-card', profile: 'body', where: '说明性长文（白卡）' },
  { fg: '--lc-ink-soft', bg: '--lc-paper', profile: 'body', where: '说明性长文（纸面）' },
  { fg: '--lc-ink-muted', bg: '--lc-paper-card', profile: 'secondary', where: '次要信息（白卡）' },
  { fg: '--lc-ink-muted', bg: '--lc-paper', profile: 'secondary', where: '次要信息（纸面）' },
  { fg: '--lc-ink-faint', bg: '--lc-paper-card', profile: 'secondary', where: 'kicker/提示/脚注（白卡）' },
  { fg: '--lc-ink-faint', bg: '--lc-paper', profile: 'secondary', where: 'kicker/提示/脚注（纸面）' },
  {
    fg: '--lc-ink-disabled',
    bg: '--lc-paper-card',
    profile: 'exempt',
    where: '禁用态文字（WCAG 1.4.3 豁免 inactive 组件）',
  },
  // —— 主色与语义色 ——
  { fg: '--lc-brand', bg: '--lc-paper-card', profile: 'body', where: '主色文字/数字（白卡）' },
  { fg: '--lc-brand', bg: '--lc-paper', profile: 'body', where: '主色文字（纸面）' },
  { fg: '#ffffff', bg: '--lc-brand', profile: 'body', where: '主按钮白字' },
  { fg: '--lc-brand-soft', bg: '--lc-brand', profile: 'decorative', where: '激活项浅底（非文字）' },
  { fg: '--lc-danger', bg: '--lc-paper-card', profile: 'body', where: '危险操作文字（白卡）' },
  // —— 装饰 ——
  { fg: '--lc-grid-line', bg: '--lc-paper', profile: 'decorative', where: '纸面网格线' },
]

function runContrast() {
  console.log('\n① 对比度（WCAG 2.1 相对亮度）')
  const targets = [
    { name: '桌面端', file: path.join(ROOT, 'admintem-vue', 'src', 'styles', 'tokens.css') },
    { name: '移动端', file: path.join(ROOT, 'admintem-mobile', 'src', 'styles', 'tokens.css') },
  ]
  const minRatio = { body: 4.5, secondary: 3.0 }

  for (const t of targets) {
    const tokens = parseTokens(t.file)
    /** 取 token 值；允许字面量（如 #ffffff） */
    const value = (key) => (key.startsWith('--') ? tokens[key] : key)
    const resolved = (key) => {
      const v = value(key)
      if (!v) throw new Error(t.name + ' 缺少 token：' + key)
      return v
    }
    console.log('  [' + t.name + '] token 定义 ' + Object.keys(tokens).length + ' 个')
    for (const p of PAIRS) {
      const fgRaw = resolved(p.fg)
      const bgRaw = resolved(p.bg)
      const bgFlat = bgRaw.startsWith('#') ? bgRaw : bgRaw
      const fgRgb = flatten(fgRaw, bgFlat)
      const bgRgb = flatten(bgRaw, '#ffffff')
      const ratio = contrast(fgRgb, bgRgb)
      const shown = ratio.toFixed(2) + ':1'
      const threshold = minRatio[p.profile]
      if (threshold === undefined) {
        const kind = p.profile === 'exempt' ? '豁免，不设门槛' : '装饰，不设门槛'
        console.log('  INFO ' + p.where + '（' + p.fg + ' / ' + p.bg + '）= ' + shown + '（' + kind + '）')
        continue
      }
      check(p.where + ' ' + p.fg + ' / ' + p.bg + ' ≥ ' + threshold + ':1', ratio >= threshold, shown)
    }
  }
}

/* ==================== ②③ 几何与触摸目标 ==================== */

/** 颜色字符串 → RGB 三元组（支持 #hex / rgb() / rgba()） */
function cssColor(v) {
  return v.startsWith('#') ? hexToRgb(v) : rgbaToRgb(v).slice(0, 3)
}

/**
 * 实测占位符对比度：读浏览器计算样式里的 `::placeholder` 颜色，
 * 并与"输入框实际可见背景"（背景透明时向上取第一个不透明祖先）合成后计算。
 * 为什么不只看 token 表：占位符颜色同时受组件库默认值与本项目映射影响，
 * 只有实测才能证明映射真的生效（Vant 默认 #c8c9cc 仅约 1.9:1）。
 */
async function placeholderInfo(page, selector) {
  return page.evaluate((sel) => {
    const el = document.querySelector(sel)
    if (!el) return null
    const color = getComputedStyle(el, '::placeholder').color
    let node = el
    let bg = 'rgba(0, 0, 0, 0)'
    while (node) {
      const c = getComputedStyle(node).backgroundColor
      if (c && !/^rgba\(0, 0, 0, 0\)$/.test(c) && c !== 'transparent') {
        bg = c
        break
      }
      node = node.parentElement
    }
    return { color, bg, text: el.getAttribute('placeholder') || '' }
  }, selector)
}

/** 执行占位符断言（门槛按正文 4.5:1） */
async function checkPlaceholder(page, label, selector) {
  const info = await placeholderInfo(page, selector)
  if (!info) {
    check(label + '：找到占位符元素', false, '选择器未命中 ' + selector)
    return
  }
  const ratio = contrast(cssColor(info.color), cssColor(info.bg))
  check(
    label + ' 占位符对比度 ≥ 4.5:1',
    ratio >= 4.5,
    info.color + ' on ' + info.bg + ' = ' + ratio.toFixed(2) + ':1'
  )
}

/** 页面级横向溢出 + 未被裁剪却超出视口的元素（排除自身可横向滚动的容器内部） */
async function geometry(page) {
  return page.evaluate(() => {
    const de = document.documentElement
    const vw = window.innerWidth
    const scroller = (el) => {
      let p = el.parentElement
      while (p && p !== document.body && p !== de) {
        const ox = getComputedStyle(p).overflowX
        if (ox === 'auto' || ox === 'scroll' || ox === 'hidden') return true
        p = p.parentElement
      }
      return false
    }
    const offenders = []
    document.querySelectorAll('body *').forEach((el) => {
      const b = el.getBoundingClientRect()
      if (b.width < 1 || b.height < 1) return
      if (b.right <= vw + 2 && b.left >= -2) return
      if (scroller(el)) return // 表格等自带横向滚动的区域不算页面溢出
      const cls = typeof el.className === 'string' && el.className ? '.' + el.className.split(' ')[0] : ''
      offenders.push(el.tagName.toLowerCase() + cls + '@' + Math.round(b.left) + '..' + Math.round(b.right))
    })
    return {
      scrollW: de.scrollWidth,
      clientW: de.clientWidth,
      vw,
      offenders: offenders.slice(0, 4),
      offenderCount: offenders.length,
    }
  })
}

async function waitFor(page, fn, timeout = 8000) {
  const deadline = Date.now() + timeout
  while (Date.now() < deadline) {
    if (await page.evaluate(fn)) return true
    await page.waitForTimeout(150)
  }
  return false
}

async function desktopLogin(page) {
  await page.goto(DESKTOP + '/login', { waitUntil: 'networkidle' })
  await page.fill('input[placeholder="admin"]', 'admin')
  await page.fill('input[type="password"]', '12345678')
  await page.click('button:has-text("登 录")')
  await waitFor(page, () => !!localStorage.getItem('admintem_token') && !location.pathname.includes('/login'))
}

async function mobileLogin(page) {
  await page.goto(MOBILE + '/#/login', { waitUntil: 'networkidle' })
  await page.fill('input[name="username"]', 'admin')
  await page.fill('input[name="password"]', '12345678')
  await page.click('button:has-text("登 录")')
  return waitFor(page, () => location.hash.includes('/home'))
}

/** 采集"可见元素的最小高度"，用于触摸目标检查 */
function minHeights(page, selector) {
  return page.evaluate((sel) => {
    const out = []
    document.querySelectorAll(sel).forEach((el) => {
      const b = el.getBoundingClientRect()
      const st = getComputedStyle(el)
      if (b.width < 1 || b.height < 1 || st.visibility === 'hidden' || st.display === 'none') return
      out.push(Math.round(b.height))
    })
    return out
  }, selector)
}

async function runDesktop() {
  console.log('\n② 桌面端适配（1280 / 1440 / 1920）')
  const browser = await chromium.launch({ executablePath: EDGE, headless: true })
  const sizes = [
    { w: 1280, h: 800 },
    { w: 1440, h: 900 },
    { w: 1920, h: 1080 },
  ]
  // 认证页：必须在"未登录"上下文里访问——已登录时路由守卫会把 /login 重定向到工作台，
  // 那样量到的是工作台而不是登录页（首版脚本就踩了这个坑，表现为"找不到占位符元素"）。
  const publicPages = [
    { p: '/login', name: '登录页' },
    { p: '/register', name: '注册页' },
  ]
  const privatePages = [
    { p: '/', name: '工作台' },
    { p: '/userInfo', name: '用户管理' },
    { p: '/roleInfo', name: '角色管理' },
    { p: '/permissionInfo', name: '权限管理' },
    { p: '/403', name: '无权限页' },
    { p: '/no-such-page', name: '404 兜底' },
  ]
  for (const s of sizes) {
    // —— 未登录：认证页 ——
    const anonCtx = await browser.newContext({ viewport: { width: s.w, height: s.h }, locale: 'zh-CN' })
    const anon = await anonCtx.newPage()
    for (const item of publicPages) {
      await anon.goto(DESKTOP + item.p, { waitUntil: 'networkidle' })
      await anon.waitForTimeout(600)
      const g = await geometry(anon)
      const label = s.w + '×' + s.h + ' ' + item.name
      check(label + '：无横向滚动', g.scrollW <= g.clientW + 1, 'scrollW ' + g.scrollW + ' / clientW ' + g.clientW)
      check(
        label + '：无超出视口元素',
        g.offenderCount === 0,
        g.offenderCount === 0 ? '' : g.offenderCount + ' 个，例：' + g.offenders.join(' , ')
      )
      await checkPlaceholder(anon, label, 'input[placeholder]')
    }
    await anonCtx.close()

    // —— 已登录：业务页 ——
    const ctx = await browser.newContext({ viewport: { width: s.w, height: s.h }, locale: 'zh-CN' })
    const page = await ctx.newPage()
    await desktopLogin(page)
    for (const item of privatePages) {
      await page.goto(DESKTOP + item.p, { waitUntil: 'networkidle' })
      await page.waitForTimeout(600)
      const g = await geometry(page)
      const label = s.w + '×' + s.h + ' ' + item.name
      check(label + '：无横向滚动', g.scrollW <= g.clientW + 1, 'scrollW ' + g.scrollW + ' / clientW ' + g.clientW)
      check(
        label + '：无超出视口元素',
        g.offenderCount === 0,
        g.offenderCount === 0 ? '' : g.offenderCount + ' 个，例：' + g.offenders.join(' , ')
      )
    }
    await ctx.close()
  }
  await browser.close()
}

async function runMobile() {
  console.log('\n③ 移动端适配（360 / 390 / 430）与触摸目标 ≥ 44px')
  const browser = await chromium.launch({ executablePath: EDGE, headless: true })
  const sizes = [
    { w: 360, h: 640, tag: '360（窄屏下限）' },
    { w: 390, h: 844, tag: '390（iPhone 常规）' },
    { w: 430, h: 932, tag: '430（大屏手机）' },
  ]
  for (const s of sizes) {
    const ctx = await browser.newContext({
      viewport: { width: s.w, height: s.h },
      deviceScaleFactor: 2,
      isMobile: true,
      hasTouch: true,
      locale: 'zh-CN',
    })
    const page = await ctx.newPage()

    // 登录页
    await page.goto(MOBILE + '/#/login', { waitUntil: 'networkidle' })
    await page.waitForTimeout(600)
    let g = await geometry(page)
    check(s.tag + ' 登录页：无横向滚动', g.scrollW <= g.clientW + 1, 'scrollW ' + g.scrollW + ' / clientW ' + g.clientW)
    check(
      s.tag + ' 登录页：无超出视口元素',
      g.offenderCount === 0,
      g.offenderCount === 0 ? '' : g.offenders.join(' , ')
    )
    const submitH = await minHeights(page, '.m-login__submit')
    check(s.tag + ' 登录按钮触摸高度 ≥ 44px', submitH.length > 0 && Math.min(...submitH) >= 44, '实测 ' + submitH.join('/') + 'px')
    await checkPlaceholder(page, s.tag + ' 登录页', 'input[name="username"]')

    // 登录后：首页 + 我的
    const ok = await mobileLogin(page)
    check(s.tag + ' 登录成功进入 #/home', ok, page.url())
    for (const v of [
      { hash: '#/home', name: '首页', sel: '.home-entry' },
      { hash: '#/mine', name: '我的', sel: '.van-button' },
    ]) {
      await page.evaluate((h) => (location.hash = h), v.hash)
      await page.waitForTimeout(900)
      g = await geometry(page)
      check(s.tag + ' ' + v.name + '：无横向滚动', g.scrollW <= g.clientW + 1, 'scrollW ' + g.scrollW + ' / clientW ' + g.clientW)
      check(
        s.tag + ' ' + v.name + '：无超出视口元素',
        g.offenderCount === 0,
        g.offenderCount === 0 ? '' : g.offenders.join(' , ')
      )
      const hs = await minHeights(page, v.sel)
      if (hs.length) {
        check(s.tag + ' ' + v.name + ' ' + v.sel + ' 触摸高度 ≥ 44px', Math.min(...hs) >= 44, '最小 ' + Math.min(...hs) + 'px（共 ' + hs.length + ' 个）')
      }
    }

    // 底部标签栏（固定在 TabLayout 上，切到任意 tab 都在）
    const tabHs = await minHeights(page, '.van-tabbar-item')
    check(
      s.tag + ' 标签栏项触摸高度 ≥ 44px',
      tabHs.length >= 2 && Math.min(...tabHs) >= 44,
      '实测 ' + tabHs.join('/') + 'px'
    )
    const tabBottom = await page.evaluate(() => {
      const el = document.querySelector('.van-tabbar')
      if (!el) return -1
      const b = el.getBoundingClientRect()
      return Math.round(window.innerHeight - b.bottom)
    })
    check(s.tag + ' 标签栏贴合底部（无露出）', tabBottom >= -2 && tabBottom <= 2, '距底 ' + tabBottom + 'px')
    await ctx.close()
  }
  await browser.close()
}

/* ==================== 入口 ==================== */
;(async () => {
  if (!fs.existsSync(EDGE)) throw new Error('未找到 Edge：' + EDGE)
  runContrast()
  await runDesktop()
  await runMobile()
  console.log('\n结果：PASS ' + passed + ' 项 / FAIL ' + failed + ' 项')
  process.exit(failed === 0 ? 0 : 1)
})().catch((e) => {
  console.error('执行失败：' + (e && e.stack ? e.stack : e))
  process.exit(1)
})
