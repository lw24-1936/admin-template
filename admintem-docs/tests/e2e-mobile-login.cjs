/*
 * 移动端 E2E：真实 Edge headless（手机视口 390x844）验证登录 -> 首页 -> 我的 -> 退出登录
 *
 * 运行前提：
 *   1. 后端运行于 8090（dev profile），移动端 dev 运行于 5174（admintem-mobile: npm run dev）；
 *   2. 浏览器：优先本机 Edge；无 Edge 时回退 Playwright Chromium；
 *   3. playwright-core 可被解析：优先本目录 node_modules，其次全局 playwright 自带副本。
 * 运行：node e2e-mobile-login.cjs
 *
 * 覆盖点：
 *   A. 登录页在手机视口下正常渲染（标题、账号/密码输入、登录按钮）
 *   B. 真实填写表单并点击“登 录”，跳转到 #/home
 *   C. 底部标签栏存在“首页/我的”两项，切换可跳转 #/mine
 *   D. 首页展示会话数据与按权限渲染的管理入口（admin 可见 4 项）
 *   E. “我的”展示账号/角色信息，退出登录后回到 #/login
 *   F. test 账号登录：管理入口按 permission_key 收敛（验证按钮级权限）
 *   G. 全程收集控制台错误
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
const HAS_EDGE = fs.existsSync(EDGE)
// 本地优先真实 Edge；CI/Linux 回退到 Playwright Chromium。
const LAUNCH_OPTIONS = HAS_EDGE
  ? { executablePath: EDGE, headless: true }
  : { headless: true }
// 基址可由环境变量覆盖（原因见 e2e-admin-login.cjs 同名注释）
// 优先用 MOBILE_BASE_URL：桌面与移动的 E2E 常共用 E2E_BASE_URL，
// 混用时会把移动端脚本指到桌面端口（曾因此误报「标题不是移动端登录」）
const APP = process.env.MOBILE_BASE_URL || process.env.E2E_BASE_URL || 'http://localhost:5174'

/* 断言辅助：失败即中断并退出码 1 */
let failed = 0
function check(label, ok, extra) {
  if (ok) {
    console.log('  PASS ' + label + (extra ? ' -> ' + extra : ''))
  } else {
    failed++
    console.log('  FAIL ' + label + (extra ? ' -> ' + extra : ''))
  }
}

/** 等待 hash 路由到达指定路径 */
async function waitHash(page, hash, timeout = 8000) {
  const deadline = Date.now() + timeout
  while (Date.now() < deadline) {
    if (page.url().includes(hash)) return true
    await page.waitForTimeout(150)
  }
  return false
}

;(async () => {
  if (!HAS_EDGE) console.log('  INFO 未找到 Edge，使用 Playwright Chromium')
  const browser = await chromium.launch(LAUNCH_OPTIONS)
  const context = await browser.newContext({
    viewport: { width: 390, height: 844 },
    deviceScaleFactor: 3,
    isMobile: true,
    hasTouch: true,
    locale: 'zh-CN',
  })
  const page = await context.newPage()
/*
 * 默认超时放宽到 60s：Vite dev server 首次被访问时要做依赖预构建与模块编译，
 * 冷启动可能超过 Playwright 默认的 30s，表现为"页面打开了但输入框还没渲染"。
 * 这是**测试环境**的冷启动问题，不是应用缺陷，因此在这里放宽而不是改应用。
 */
page.setDefaultTimeout(60000)
await page.goto(APP + '/#/login', { waitUntil: 'domcontentloaded' })
await page.waitForSelector('input', { timeout: 60000 })
  const errors = []
  page.on('console', (m) => {
    if (m.type() === 'error') errors.push(m.text())
  })
  page.on('pageerror', (e) => errors.push('pageerror: ' + e.message))
  page.on('response', (res) => {
    if (res.status() >= 400) errors.push('HTTP ' + res.status() + ' ' + res.url())
  })

  /* ---------- A. 登录页渲染 ---------- */
  console.log('A. 登录页（390x844 手机视口）')
  await page.goto(APP + '/#/login', { waitUntil: 'networkidle' })
  const loginText = (await page.evaluate(() => document.body.innerText)).replace(/\s+/g, ' ')
  check('标题为移动端登录', loginText.includes('移动端登录'))
  check('存在账号/密码输入框', (await page.locator('input').count()) >= 2)
  check('存在登录按钮', await page.locator('button:has-text("登 录")').count() > 0)
  check('页面宽度未溢出（无横向滚动）', await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth + 1))

  /* ---------- B/C/D. admin 登录并进入首页 ---------- */
  console.log('B. admin 真实表单登录')
  await page.fill('input[name="username"]', 'admin')
  await page.fill('input[name="password"]', '12345678')
  await page.click('button:has-text("登 录")')
  check('登录后跳转 #/home', await waitHash(page, '#/home'), page.url())

  console.log('C. 底部标签栏')
  const tabText = (await page.evaluate(() => document.body.innerText)).replace(/\s+/g, ' ')
  check('标签栏含“首页”', tabText.includes('首页'))
  check('标签栏含“我的”', tabText.includes('我的'))

  console.log('D. 首页内容与权限入口')
  check(
    '显示欢迎语',
    /(夜深了|上午好|中午好|下午好|晚上好)，admin/.test(tabText),
    tabText.slice(0, 80)
  )
  check('角色展示为角色名（非 [object Object]）', !tabText.includes('[object Object]'))
  const entryCount = await page.locator('.home-entry').count()
  check('admin 可见 4 个管理入口', entryCount === 4, '实际 ' + entryCount)
  check('管理入口含用户管理', (await page.locator('.home-entry:has-text("用户管理")').count()) === 1)
  check('首页展示可访问菜单', tabText.includes('我的菜单'))

  /* ---------- E. 我的 + 退出登录 ---------- */
  console.log('E. 我的页与退出登录')
  await page.click('.van-tabbar-item:has-text("我的")')
  check('切换到 #/mine', await waitHash(page, '#/mine'), page.url())
  const mineText = (await page.evaluate(() => document.body.innerText)).replace(/\s+/g, ' ')
  check('展示登录账号 admin', mineText.includes('admin'))
  check('展示角色标签 admin', mineText.includes('admin'))
  check('存在退出登录按钮', await page.locator('button:has-text("退出登录")').count() > 0)
  console.log('E2. 资料编辑弹层与头像选择器')
  await page.click('.van-cell:has-text("编辑资料")')
  await page.waitForSelector('.profile-avatar-btn')
  check('资料编辑弹层已打开', (await page.locator('input[name="nickName"]').count()) === 1)
  check('昵称已回填当前账号', (await page.inputValue('input[name="nickName"]')) === 'admin')
  const chooserPromise = page.waitForEvent('filechooser', { timeout: 2500 }).catch(() => null)
  await page.click('.profile-avatar-btn')
  const chooser = await chooserPromise
  check('头像按钮可打开文件选择器', !!chooser)
  await page.locator('.van-popup:visible button:has-text("取消")').last().click()
  await page.waitForTimeout(300)


  await page.click('button:has-text("退出登录")')
  await page.waitForTimeout(300)
  check('弹出二次确认', await page.locator('.van-dialog:has-text("退出登录")').count() > 0)
  await page.click('.van-dialog__confirm')
  check('退出后回到 #/login', await waitHash(page, '#/login'), page.url())
  check('本地令牌已清理', (await page.evaluate(() => localStorage.getItem('admintem_token'))) === null)

  /* ---------- F. test 账号：按钮级权限收敛 ---------- */
  console.log('F. test 账号权限收敛')
  await page.fill('input[name="username"]', 'test')
  await page.fill('input[name="password"]', '12345678')
  await page.click('button:has-text("登 录")')
  check('test 登录进入 #/home', await waitHash(page, '#/home'), page.url())
  await page.waitForTimeout(600)
  const testEntryCount = await page.locator('.home-entry').count()
  const testUserEntry = await page.locator('.home-entry:has-text("用户管理")').count()
  const testAddEntry = await page.locator('.home-entry:has-text("新增用户")').count()
  console.log('  INFO test 可见管理入口：' + testEntryCount + '（用户管理 ' + testUserEntry + ' / 新增用户 ' + testAddEntry + '）')
  check('test 不越权显示“新增用户”按钮入口', testAddEntry === 0)
  check('test 管理入口少于 admin', testEntryCount < entryCount, testEntryCount + ' < ' + entryCount)

  /* ---------- H. 移动端布局几何校验 ---------- */
  console.log('H. 移动端布局几何校验')
  await page.click('.van-tabbar-item:has-text("首页")')
  await waitHash(page, '#/home')
  await page.waitForTimeout(400)
  const geo = await page.evaluate(() => {
    // 滚动到页底后测量，确认最后一张卡片不会被固定标签栏遮挡
    window.scrollTo(0, document.documentElement.scrollHeight)
    const bar = document.querySelector('.van-tabbar')
    const r = bar.getBoundingClientRect()
    const cards = document.querySelectorAll('.lc-card')
    const last = cards.length ? cards[cards.length - 1].getBoundingClientRect() : null
    const entry = document.querySelector('.home-entry')
    return {
      barLeft: Math.round(r.left),
      barRight: Math.round(r.right),
      barBottom: Math.round(r.bottom),
      barHeight: Math.round(r.height),
      viewportW: window.innerWidth,
      viewportH: window.innerHeight,
      lastCardBottom: last ? Math.round(last.bottom) : null,
      entryHeight: entry ? Math.round(entry.getBoundingClientRect().height) : null,
      overflow: document.documentElement.scrollWidth > window.innerWidth + 1,
      position: getComputedStyle(bar).position,
    }
  })
  console.log('  INFO ' + JSON.stringify(geo))
  check('标签栏为 fixed 定位', geo.position === 'fixed')
  check('标签栏贴合视口底部', Math.abs(geo.barBottom - geo.viewportH) <= 1)
  check('标签栏占满手机宽度', geo.barLeft === 0 && geo.barRight === geo.viewportW)
  check('标签栏高度符合 50px 设定', geo.barHeight >= 48 && geo.barHeight <= 62, geo.barHeight + 'px')
  check('内容区未被标签栏遮挡', geo.lastCardBottom === null || geo.lastCardBottom <= geo.viewportH - geo.barHeight + 1)
  check('管理入口触摸区域高度达标', geo.entryHeight === null || geo.entryHeight >= 44, geo.entryHeight + 'px')
  check('首页无横向溢出', !geo.overflow)

  console.log('G. 控制台错误：' + (errors.length ? JSON.stringify(errors.slice(0, 5)) : 'none'))
  check('无控制台错误', errors.length === 0)

  /* ---------- I. 令牌失效处理（放在错误检查之后，401 属预期响应） ---------- */
  console.log('I. 令牌失效与重定向回环检查')
  await page.evaluate(() => {
    // 模拟“会话彻底失效”：只改 access token 会触发 refreshToken 自动续期，
    // 这属于新增的预期能力，因此这里同时清掉刷新令牌，覆盖真正的失效路径。
    localStorage.setItem('admintem_token', 'expired.invalid.token')
    localStorage.removeItem('admintem_refresh_token')
  })
  // 必须整页重载：只有重新加载才会在守卫中校验令牌（hash 变更不会重新初始化应用）
  await page.goto(APP + '/#/home')
  await page.reload({ waitUntil: 'networkidle' })
  await page.waitForTimeout(1500)
  check('失效令牌已被清理', (await page.evaluate(() => localStorage.getItem('admintem_token'))) === null)
  check('失效后停留登录页', page.url().includes('#/login'), page.url())
  await page.waitForTimeout(1200)
  check('1.2s 后仍在登录页（无重定向回环）', page.url().includes('#/login'), page.url())
  console.log('  INFO 本阶段预期内响应：' + JSON.stringify(errors.slice(0, 2)))

  /* ---------- J. 修改密码全流程（用临时账号，避免影响演示账号） ---------- */
  console.log('J. 修改密码全流程（临时账号）')
  // 用管理员令牌建一个临时账号（接口调用在页面上下文内完成，复用 CORS 与后端）
  const stamp = Date.now().toString().slice(-8)
  const tmpUser = 'e2e_m_' + stamp
  const oldPwd = 'Passw0rd!1'
  const newPwd = 'Passw0rd!2'
  async function apiCall(path, method, body, token) {
    return page.evaluate(
      async ([p, m, b, t]) => {
        const res = await fetch('http://localhost:8090' + p, {
          method: m,
          headers: { 'Content-Type': 'application/json', admintem_token: 'LAUNCH ' + t },
          body: b ? JSON.stringify(b) : undefined,
        })
        return await res.json()
      },
      [path, method, body, token]
    )
  }
  const adminLogin = await page.evaluate(async () => {
    const res = await fetch('http://localhost:8090/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: 'admin', password: '12345678', rememberMe: false }),
    })
    return (await res.json()).data.token
  })
  const created = await apiCall('/sysAuthUser/addSysAuthUser', 'POST', {
    userName: tmpUser, nickName: '移动端改密验证', password: oldPwd, status: 0,
  }, adminLogin)
  check('创建临时账号成功', created.code === 200, 'code=' + created.code)
  const tmpUserId = created.data

  // 用临时账号登录移动端并进入「我的」
  await page.goto(APP + '/#/login')
  await page.reload({ waitUntil: 'networkidle' })
  await page.fill('input[name="username"]', tmpUser)
  await page.fill('input[name="password"]', oldPwd)
  await page.click('button:has-text("登 录")')
  check('临时账号登录进入首页', await waitHash(page, '#/home'), page.url())
  await page.click('.van-tabbar-item:has-text("我的")')
  check('切换到我的页', await waitHash(page, '#/mine'), page.url())

  // 打开改密弹层并提交（两次新密码一致）
  await page.click('.van-cell:has-text("修改密码")')
  await page.waitForTimeout(800)
  check('改密弹层已打开', (await page.locator('.van-popup:visible').count()) > 0)
  const fields = page.locator('.van-popup:visible input')
  await fields.nth(0).fill(oldPwd)
  await fields.nth(1).fill(newPwd)
  await fields.nth(2).fill(newPwd)
  await page.click('.van-popup:visible button:has-text("确认修改")')
  check('改密后强制回到登录页', await waitHash(page, '#/login', 10000), page.url())
  check('本地令牌已清理', (await page.evaluate(() => localStorage.getItem('admintem_token'))) === null)

  // 旧密码应失败、新密码应成功
  await page.fill('input[name="username"]', tmpUser)
  await page.fill('input[name="password"]', oldPwd)
  await page.click('button:has-text("登 录")')
  await page.waitForTimeout(1500)
  check('旧密码已不可登录', page.url().includes('#/login'), page.url())
  await page.fill('input[name="username"]', tmpUser)
  await page.fill('input[name="password"]', newPwd)
  await page.click('button:has-text("登 录")')
  check('新密码可登录', await waitHash(page, '#/home', 10000), page.url())

  // 清理临时账号（逻辑删除）
  const cleaned = await apiCall('/sysAuthUser/deleteById', 'POST', { id: tmpUserId }, adminLogin)
  check('临时账号已清理', cleaned.code === 200, 'code=' + cleaned.code)

  await browser.close()
  console.log(failed === 0 ? '\n移动端 E2E 全部通过' : '\n移动端 E2E 失败项：' + failed)
  process.exit(failed === 0 ? 0 : 1)
})().catch((e) => {
  console.error('E2E FAILED', e)
  process.exit(1)
})
