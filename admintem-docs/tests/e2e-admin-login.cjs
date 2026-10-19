/*
 * 桌面端 E2E：真实 Edge headless 验证登录 -> 工作台 -> 全部业务页面与弹层
 *
 * 运行前提：
 *   1. 后端运行于 8090（dev profile），桌面端 dev 运行于 5173（admintem-vue: npm run dev）；
 *   2. 本机装有 Edge（默认路径 C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe）；
 *   3. playwright-core 可被解析：优先本目录 node_modules，其次全局 playwright 自带副本
 *      （无需再手动 npm i playwright-core）。
 * 运行：node e2e-admin-login.cjs
 *
 * 覆盖点：
 *   A. 登录页渲染并可用真实表单登录
 *   B. 登录后进入工作台（动态路由首个可访问菜单）
 *   C. 侧边栏出现授权菜单
 *   D. 用户管理：列表渲染、分页中文文案、新增/分配角色/重置密码弹层可打开并取消
 *   E. 角色管理：列表渲染、分配权限弹层（含权限树）、新增角色弹层
 *   F. 权限管理：列表渲染、权限树渲染、新增权限弹层
 *   G. 无权限页 /403 正常渲染
 *   H. 全程无控制台错误、无"组件未解析"告警（按需引入的回归门槛）
 *
 * 说明：脚本只打开弹层后取消，不写入任何数据。
 */
const fs = require('fs')

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
// 基址允许用环境变量覆盖：本机常有其他项目的 dev server 也监听 5173
// （Windows 下两个进程可以同时绑定同一端口），硬编码会让 E2E 打到别人的页面上，
// 表现为"选择器等不到"这种看似脚本损坏、实为环境串台的假故障。
const APP = process.env.E2E_BASE_URL || 'http://localhost:5173'

let failed = 0
function check(label, ok, extra) {
  if (ok) {
    console.log('  PASS ' + label + (extra ? ' -> ' + extra : ''))
  } else {
    failed++
    console.log('  FAIL ' + label + (extra ? ' -> ' + extra : ''))
  }
}

;(async () => {
  if (!fs.existsSync(EDGE)) throw new Error('未找到 Edge：' + EDGE)
  const browser = await chromium.launch({ executablePath: EDGE, headless: true })
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } })
// 同移动端：dev server 冷启动可能超过默认 30s，放宽到 60s
page.setDefaultTimeout(60000)
  const errors = []
  const warnings = []
  page.on('console', (m) => {
    if (m.type() === 'error') errors.push(m.text())
    if (m.type() === 'warning') warnings.push(m.text())
  })
  page.on('pageerror', (e) => errors.push('pageerror: ' + e.message))

  /** 打开某页面并等待渲染 */
  async function goto(path) {
    await page.goto(APP + path, { waitUntil: 'networkidle' })
    await page.waitForTimeout(1500)
  }

  /** 当前页面可见文本（压缩空白） */
  async function text() {
    return (await page.evaluate(() => document.body.innerText)).replace(/\s+/g, ' ')
  }

  /** 打开弹层并取消（返回弹层是否出现）；仅操作可见弹层，避免命中隐藏弹层的按钮 */
  async function openDialogAndCancel(triggerLocator) {
    const count = await triggerLocator.count()
    if (!count) return false
    await triggerLocator.first().click()
    const dialog = page.locator('.el-dialog:visible').first()
    let visible = false
    try {
      await dialog.waitFor({ state: 'visible', timeout: 3000 })
      visible = true
    } catch (e) {
      return false
    }
    // 优先点“取消”，其次点右上角关闭，最后用 ESC
    const cancelBtn = dialog.locator('.el-dialog__footer button:has-text("取消")')
    const closeBtn = dialog.locator('.el-dialog__headerbtn')
    if (await cancelBtn.count()) {
      await cancelBtn.first().click()
    } else if (await closeBtn.count()) {
      await closeBtn.first().click()
    } else {
      await page.keyboard.press('Escape')
    }
    await page.waitForTimeout(500)
    return visible
  }

  /* ---------- A. 登录页 ---------- */
  console.log('A. 登录页')
  await page.goto(APP + '/login', { waitUntil: 'networkidle' })
  check('标题为 admintem 登录页', (await page.title()).includes('登录'), await page.title())
  await page.fill('input[placeholder="admin"]', 'admin')
  await page.fill('input[type="password"]', '12345678')
  await page.click('button:has-text("登 录")')
  await page.waitForTimeout(2500)

  /* ---------- B. 工作台 ---------- */
  console.log('B. 登录后进入工作台')
  const homeText = await text()
  check('进入工作台（URL 非 /login）', !page.url().includes('/login'), page.url())
  check('工作台内容已渲染', homeText.includes('工作台') || homeText.includes('admin'), homeText.slice(0, 60))

  /* ---------- C. 侧边栏菜单 ---------- */
  console.log('C. 侧边栏菜单')
  const groups = page.locator('.el-sub-menu__title')
  for (let i = 0, n = await groups.count(); i < n; i++) {
    await groups.nth(i).click().catch(() => {})
    await page.waitForTimeout(150)
  }
  await page.waitForTimeout(400)
  const menuText = await text()
  check('侧边栏含系统管理', menuText.includes('系统管理'))
  check('侧边栏含用户管理', menuText.includes('用户管理'))
  check('侧边栏含角色管理', menuText.includes('角色管理'))
  check('侧边栏含权限管理', menuText.includes('权限管理'))

  /* ---------- D. 用户管理 ---------- */
  console.log('D. 用户管理页')
  await goto('/userInfo')
  const userText = await text()
  check('列表已渲染（含表头与账号）', userText.includes('账号') && userText.includes('admin'), userText.slice(0, 60))
  check('未被 404/403 拦截', !userText.includes('页面不存在') && !userText.includes('无权访问'), page.url())
  check('分页为中文文案', userText.includes('共') && (userText.includes('条') || userText.includes('前往')), '')
  check('新增用户弹层可打开并取消', await openDialogAndCancel(page.locator('button:has-text("新增用户")')))
  check('分配角色弹层可打开并取消', await openDialogAndCancel(page.locator('.el-table__row').first().locator('button:has-text("分配角色")')))
  check('重置密码弹层可打开并取消', await openDialogAndCancel(page.locator('.el-table__row').first().locator('button:has-text("重置密码")')))

  /* ---------- E. 角色管理 ---------- */
  console.log('E. 角色管理页')
  await goto('/roleInfo')
  const roleText = await text()
  check('角色列表已渲染', roleText.includes('角色') && roleText.includes('admin'), roleText.slice(0, 60))
  check('分配权限弹层（权限树）可打开并取消', await openDialogAndCancel(page.locator('.el-table__row').first().locator('button:has-text("分配权限")')))
  check('新增角色弹层可打开并取消', await openDialogAndCancel(page.locator('button:has-text("新增角色")')))

  /* ---------- F. 权限管理 ---------- */
  console.log('F. 权限管理页')
  await goto('/permissionInfo')
  const permText = await text()
  check('权限树已渲染（含工作台节点）', permText.includes('工作台') || permText.includes('系统管理'), permText.slice(0, 60))
  check('新增权限弹层可打开并取消', await openDialogAndCancel(page.locator('button:has-text("新增顶级权限")')))

  /* ---------- G. 无权限页 ---------- */
  console.log('G. 无权限页 /403')
  await goto('/403')
  const forbidText = await text()
  check('/403 正常渲染', forbidText.includes('无权') || forbidText.includes('403'), forbidText.slice(0, 60))

  /* ---------- H. 控制台 ---------- */
  const unresolved = warnings.filter((w) => w.includes('Failed to resolve component'))
  console.log('H. 控制台错误：' + (errors.length ? JSON.stringify(errors.slice(0, 5)) : 'none'))
  console.log('  组件未解析告警：' + (unresolved.length ? JSON.stringify(unresolved.slice(0, 5)) : 'none'))
  check('无控制台错误', errors.length === 0)
  check('无组件未解析告警（按需引入完整性）', unresolved.length === 0)

  await browser.close()
  console.log(failed === 0 ? '\n桌面端 E2E 全部通过' : '\n桌面端 E2E 失败项：' + failed)
  process.exit(failed === 0 ? 0 : 1)
})().catch((e) => {
  console.error('E2E FAILED', e)
  process.exit(1)
})
