/*
 * 全站页面截图脚本（用于《12-页面与界面说明》文档配图）
 *
 * 运行前提：
 *   1. 后端运行于 8090、桌面端 dev 运行于 5175、移动端 dev 运行于 5174；
 *   2. 本机装有 Edge；playwright-core 可被解析（优先本目录，其次全局 playwright 自带副本）。
 * 运行：node capture-screenshots.cjs
 *
 * 产物：admintem-docs/images/*.png（按固定命名，供文档引用；本脚本可重复执行以刷新截图）
 * 说明：本脚本只做"打开页面 + 截图 + 关闭弹层"，不提交任何表单，不产生业务数据。
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
const DESKTOP = process.env.DESKTOP_BASE_URL || 'http://localhost:5175'
const MOBILE = process.env.MOBILE_BASE_URL || 'http://localhost:5174'
// 输出目录：默认写入 admintem-docs/images（文档配图）；可用 CAPTURE_OUT 覆盖（用于改版前后对比）
const OUT = process.env.CAPTURE_OUT
  ? path.resolve(process.env.CAPTURE_OUT)
  : path.resolve(__dirname, '..', 'images')

/** 截图计数（便于日志核对） */
let shotCount = 0
const manifest = []

/**
 * 隐藏 Vue DevTools 浮层（只在 dev server 下存在）。
 *
 * 为什么必须做：该浮层是"开发态注入物"，不属于产品界面，却会随机出现在截图里
 * （实测同一脚本两次运行，工作台截图底部约 80×40 区域时有时无，逐像素比对时
 * 会被误判成"设计改动"）。截图是文档配图与改版比对的基线，必须只包含产品像素。
 */
const HIDE_DEV_OVERLAY = `
  .vue-devtools__anchor, .vue-devtools__panel, #vue-devtools-anchor,
  [class*="vue-devtools"] { display: none !important; }
`

/** 在页面中屏蔽开发态浮层（幂等，可重复调用） */
async function hideDevOverlays(page) {
  await page.addStyleTag({ content: HIDE_DEV_OVERLAY }).catch(() => {})
}

/**
 * 把鼠标移到页面左上角空白处。
 *
 * 为什么：上一次 click() 之后指针停留在被点击的按钮/表单项上，会留下 hover 态
 * （背景 `--lc-surface-hover`、描边变化），导致同一页面两次截图出现几百到几千像素差异。
 * 截图前移开指针，可让"悬停态"这类非产品状态不再进入基线图。
 */
async function parkMouse(page) {
  await page.mouse.move(2, 2).catch(() => {})
  await page.waitForTimeout(200)
}

/**
 * 等待元素尺寸稳定（连续两次测量一致才认为加载完成）。
 *
 * 用途：弹层里的树/列表是异步加载的，固定 sleep 会在"数据未回来"时截图，
 * 得到高度偏小的弹层图（实测角色分配权限弹层曾出现 693px 与更矮两种结果）。
 */
async function waitStable(locator, timeoutMs = 6000) {
  const deadline = Date.now() + timeoutMs
  let prev = null
  while (Date.now() < deadline) {
    const box = await locator.boundingBox().catch(() => null)
    const key = box ? `${Math.round(box.width)}x${Math.round(box.height)}` : 'none'
    if (key === prev && key !== 'none') return true
    prev = key
    await locator.page().waitForTimeout(250)
  }
  return false
}

/** 截图并登记到清单 */
async function shot(page, name, note, options = {}) {
  const file = path.join(OUT, name + '.png')
  await hideDevOverlays(page)
  await parkMouse(page)
  await page.screenshot({ path: file, fullPage: options.fullPage === true })
  shotCount++
  const size = fs.statSync(file).size
  manifest.push({ name, note, size })
  console.log(`  [${shotCount}] ${name}.png  ${(size / 1024).toFixed(0)} KB  ${note}`)
}

/** 打开桌面端路径并等待渲染 */
async function desktopGoto(page, p) {
  await page.goto(DESKTOP + p, { waitUntil: 'networkidle' })
  await hideDevOverlays(page)
  await page.waitForTimeout(1200)
}

/**
 * 等待登录真正完成（令牌已写入且已离开登录页）。
 *
 * 说明：后端冷启动后的首次登录可能明显变慢（实测 1.8s 左右），
 * 固定 sleep 会导致后续页面被路由守卫重定向回登录页、截图全部变成登录页。
 */
async function waitLoggedIn(page, timeoutMs = 30000) {
  const deadline = Date.now() + timeoutMs
  while (Date.now() < deadline) {
    const token = await page.evaluate(() => localStorage.getItem('admintem_token'))
    if (token && !page.url().includes('/login')) return true
    await page.waitForTimeout(250)
  }
  return false
}

/** 桌面端登录（真实表单） */
async function desktopLogin(page, username, password) {
  await page.goto(DESKTOP + '/login', { waitUntil: 'networkidle' })
  await page.fill('input[placeholder="admin"]', username)
  await page.fill('input[type="password"]', password)
  await page.click('button:has-text("登 录")')
  if (!(await waitLoggedIn(page))) {
    throw new Error(`桌面端登录失败（${username}）：未取得令牌或仍停留在登录页`)
  }
  await page.waitForTimeout(800)
}

/** 展开侧边栏全部分组，保证子菜单可见 */
async function expandSidebar(page) {
  const groups = page.locator('.el-sub-menu__title')
  for (let i = 0, n = await groups.count(); i < n; i++) {
    await groups.nth(i).click().catch(() => {})
    await page.waitForTimeout(150)
  }
  await page.waitForTimeout(400)
}

/** 打开弹层 → 截图 → 关闭（不提交） */
async function shotDialog(page, trigger, name, note) {
  const count = await trigger.count()
  if (!count) {
    console.log(`  [跳过] ${name}：未找到触发按钮`)
    return
  }
  await trigger.first().click()
  const dialog = page.locator('.el-dialog:visible').first()
  try {
    await dialog.waitFor({ state: 'visible', timeout: 4000 })
  } catch (e) {
    console.log(`  [跳过] ${name}：弹层未出现`)
    return
  }
  // 等弹层尺寸稳定（内部树/列表是异步加载的），避免截到"还没长开"的弹层
  await waitStable(dialog)
  await hideDevOverlays(page)
  await parkMouse(page)
  await page.waitForTimeout(300)
  const file = path.join(OUT, name + '.png')
  await dialog.screenshot({ path: file })
  shotCount++
  const size = fs.statSync(file).size
  manifest.push({ name, note, size })
  console.log(`  [${shotCount}] ${name}.png  ${(size / 1024).toFixed(0)} KB  ${note}`)

  // 关闭：优先取消按钮，其次右上角关闭，最后 ESC
  const cancelBtn = dialog.locator('.el-dialog__footer button:has-text("取消")')
  const closeBtn = dialog.locator('.el-dialog__headerbtn')
  if (await cancelBtn.count()) await cancelBtn.first().click()
  else if (await closeBtn.count()) await closeBtn.first().click()
  else await page.keyboard.press('Escape')
  await page.waitForTimeout(500)
}

;(async () => {
  fs.mkdirSync(OUT, { recursive: true })
  const browser = await chromium.launch({ executablePath: EDGE, headless: true })

  /* ================= 桌面端 ================= */
  console.log('桌面端（1440×900）')
  const desktopCtx = await browser.newContext({
    viewport: { width: 1440, height: 900 },
    locale: 'zh-CN',
  })
  const page = await desktopCtx.newPage()

  // 1) 登录页
  await desktopGoto(page, '/login')
  await shot(page, 'desktop-01-login', '登录页（/login）')

  // 2) 注册页
  await desktopGoto(page, '/register')
  await shot(page, 'desktop-02-register', '注册页（/register）')

  // 3) 登录后进入工作台
  await desktopLogin(page, 'admin', '12345678')
  await expandSidebar(page)
  await shot(page, 'desktop-03-home', '工作台（登录后默认进入）')

  // 4) 用户管理：列表 + 三个弹层
  await desktopGoto(page, '/userInfo')
  await shot(page, 'desktop-04-user-list', '用户管理（/userInfo）')
  await shotDialog(page, page.locator('button:has-text("新增用户")'), 'desktop-05-user-add', '用户管理 · 新增用户弹层')
  await shotDialog(page, page.locator('.el-table__row').first().locator('button:has-text("分配角色")'), 'desktop-06-user-assign-role', '用户管理 · 分配角色弹层')
  await shotDialog(page, page.locator('.el-table__row').first().locator('button:has-text("重置密码")'), 'desktop-07-user-reset-pwd', '用户管理 · 重置密码弹层')

  // 5) 角色管理：列表 + 分配权限 + 新增
  await desktopGoto(page, '/roleInfo')
  await shot(page, 'desktop-08-role-list', '角色管理（/roleInfo）')
  await shotDialog(page, page.locator('.el-table__row').first().locator('button:has-text("分配权限")'), 'desktop-09-role-assign-perm', '角色管理 · 分配权限弹层（权限树）')
  await shotDialog(page, page.locator('button:has-text("新增角色")'), 'desktop-10-role-add', '角色管理 · 新增角色弹层')

  // 6) 权限管理：树表 + 新增
  await desktopGoto(page, '/permissionInfo')
  await shot(page, 'desktop-11-permission-tree', '权限管理（/permissionInfo）')
  await shotDialog(page, page.locator('button:has-text("新增顶级权限")'), 'desktop-12-permission-add', '权限管理 · 新增权限弹层')

  // 6.1) 操作审计（V1.0.7）：列表 + 明细抽屉
  await desktopGoto(page, '/auditLog')
  await shot(page, 'desktop-16-audit-list', '操作审计（/auditLog，只读查询）')
  await page.locator('button:has-text("查看")').first().click()
  await page.waitForTimeout(600)
  await shot(page, 'desktop-17-audit-detail', '操作审计 · 明细抽屉（请求/失败原因/traceId）')

  // 7) 状态页
  await desktopGoto(page, '/403')
  await shot(page, 'desktop-13-403', '无权限页（/403）')
  await desktopGoto(page, '/no-such-page')
  await shot(page, 'desktop-14-404', '页面不存在（404 兜底）')

  // 8) 受限账号：按钮级权限效果
  await page.evaluate(() => localStorage.removeItem('admintem_token'))
  await desktopLogin(page, 'test', '12345678')
  await desktopGoto(page, '/userInfo')
  await shot(page, 'desktop-15-user-list-test', '用户管理 · 受限账号 test（写操作按钮不渲染）')

  await desktopCtx.close()

  /* ================= 移动端 ================= */
  console.log('移动端（390×844）')
  const mobileCtx = await browser.newContext({
    viewport: { width: 390, height: 844 },
    deviceScaleFactor: 2,
    isMobile: true,
    hasTouch: true,
    locale: 'zh-CN',
  })
  const m = await mobileCtx.newPage()

  await m.goto(MOBILE + '/#/login', { waitUntil: 'networkidle' })
  await m.waitForTimeout(800)
  await shot(m, 'mobile-01-login', '移动端登录页（#/login）')

  await m.fill('input[name="username"]', 'admin')
  await m.fill('input[name="password"]', '12345678')
  await m.click('button:has-text("登 录")')
  if (!(await waitLoggedIn(m))) {
    throw new Error('移动端登录失败：未取得令牌或仍停留在登录页')
  }
  await m.waitForTimeout(1200)
  await shot(m, 'mobile-02-home', '移动端首页（#/home）')

  await m.click('.van-tabbar-item:has-text("我的")')
  await m.waitForTimeout(1200)
  await shot(m, 'mobile-03-mine', '移动端我的（#/mine）')

  // 修改密码弹层
  await m.click('.van-cell:has-text("修改密码")')
  await m.waitForTimeout(900)
  const popup = m.locator('.van-popup:visible').first()
  if (await popup.count()) {
    const file = path.join(OUT, 'mobile-04-password.png')
    await waitStable(popup)
    await hideDevOverlays(m)
    await popup.screenshot({ path: file })
    shotCount++
    const size = fs.statSync(file).size
    manifest.push({ name: 'mobile-04-password', note: '移动端 · 修改密码弹层', size })
    console.log(`  [${shotCount}] mobile-04-password.png  ${(size / 1024).toFixed(0)} KB  移动端 · 修改密码弹层`)
    await m.keyboard.press('Escape')
  }

  await mobileCtx.close()
  await browser.close()

  /* ================= 汇总 ================= */
  const totalKb = manifest.reduce((sum, item) => sum + item.size, 0) / 1024
  console.log(`\n共生成 ${shotCount} 张截图，合计 ${totalKb.toFixed(0)} KB -> ${OUT}`)
  fs.writeFileSync(path.join(OUT, 'manifest.json'), JSON.stringify(manifest, null, 2), 'utf8')
  console.log('清单已写入 images/manifest.json')
})().catch((e) => {
  console.error('截图失败：', e)
  process.exit(1)
})
