/*
 * 生成《05-接口与权限标识对照表》第 9 节「角色可访问矩阵」
 *
 * 为什么需要它：交付/评审时最常被问到的问题是"这个角色到底能做什么"，
 * 而这个答案散在三处——角色的授权（db/init 种子里的 role_permission）、
 * 菜单与按钮的定义（permission 种子）、接口的鉴权注解（@PreAuthorize）。
 * 人工汇总必然漂移，因此做成"从种子与代码推导"的生成器。
 *
 * 特点：
 *   1. **纯静态**：不连数据库、不启动后端，直接解析 db/init 种子与 Java 注解，
 *      因此可以在 CI 或任何机器上复现；
 *   2. **不改业务逻辑**：只在《05》文档的两个标记之间写入生成内容；
 *   3. **支持校验模式**：`--check` 只比较不写入，退出码 1 表示文档已过期，
 *      适合作为提交前卡点（与 check-permission-doc-consistency.cjs 同一思路）。
 *
 * 用法（在 admintem-docs/tests 目录）：
 *   node gen-role-endpoint-matrix.cjs          # 生成/更新文档
 *   node gen-role-endpoint-matrix.cjs --check  # 校验文档是否为最新
 */
const fs = require('fs')
const path = require('path')

const ROOT = path.resolve(__dirname, '..', '..')
const INIT_SQL = path.join(ROOT, 'admintem-docs', 'db', 'init', '00_admintem_full_init.sql')
const DOC = path.join(ROOT, 'admintem-docs', '05-接口与权限标识对照表.md')
const BACKEND = path.join(ROOT, 'admintem-backend', 'src', 'main', 'java')
const APP_YML = path.join(ROOT, 'admintem-backend', 'src', 'main', 'resources', 'application.yml')

const BEGIN = '<!-- BEGIN:ROLE-ENDPOINT-MATRIX（由 tests/gen-role-endpoint-matrix.cjs 生成，请勿手工编辑） -->'
const END = '<!-- END:ROLE-ENDPOINT-MATRIX -->'
const CHECK_ONLY = process.argv.includes('--check')

/* ------------------------------------------------------------------ *
 * 一、解析 db/init 种子（权限 / 角色 / 授权 / 账号）
 * ------------------------------------------------------------------ */

/** 按单引号感知切分一行 VALUES（不处理转义引号：本项目种子无此写法） */
function splitFields(row) {
  const out = []
  let cur = ''
  let inQuote = false
  for (const ch of row) {
    if (ch === "'") {
      inQuote = !inQuote
      cur += ch
    } else if (ch === ',' && !inQuote) {
      out.push(cur.trim())
      cur = ''
    } else {
      cur += ch
    }
  }
  out.push(cur.trim())
  return out
}

/** 去掉两端单引号；NULL 返回 null */
function unquote(value) {
  if (value === undefined || value === null) return null
  const v = value.trim()
  if (/^NULL$/i.test(v)) return null
  if (v.startsWith("'") && v.endsWith("'")) return v.slice(1, -1)
  return v
}

/**
 * 去掉 SQL 注释。
 *
 * 必须做这一步：初始化脚本刻意在 INSERT 的多行 VALUES 之间插入了中文说明
 * （例如"测试角色1：工作台 + 系统管理…"），若不去掉，注释会被当成行首内容，
 * 导致该行解析出的角色ID 变成 NaN —— 表现为"某个角色的授权凭空消失"。
 */
function stripSqlComments(text) {
  return text
    .split(/\r?\n/)
    .map((line) => line.replace(/--.*$/, ''))
    .join('\n')
    .replace(/\/\*[\s\S]*?\*\//g, '')
}

/** 取出某个 INSERT INTO 语句的所有 VALUES 行 */
function extractValueRows(sql, table) {
  const re = new RegExp('INSERT INTO\\s+`?' + table + '`?[\\s\\S]*?VALUES([\\s\\S]*?);', 'i')
  const m = re.exec(sql)
  if (!m) throw new Error('未在初始化脚本中找到 ' + table + ' 的 INSERT 语句')
  return m[1]
    .split(/\)\s*,\s*(?=\()/) // 行与行之间是 "),("
    .map((s) => s.trim().replace(/^\(/, '').replace(/\)$/, '').trim())
    .filter(Boolean)
}

const sql = stripSqlComments(fs.readFileSync(INIT_SQL, 'utf8'))

/** 权限：id -> { id, name, type, menuUrl, backendUrl, key } */
const permissions = new Map()
for (const row of extractValueRows(sql, 'sys_auth_permission')) {
  const f = splitFields(row)
  permissions.set(Number(f[0]), {
    id: Number(f[0]),
    name: unquote(f[1]),
    parentId: Number(f[2]),
    type: Number(f[4]),
    menuUrl: unquote(f[5]),
    component: unquote(f[6]),
    backendUrl: unquote(f[7]),
    key: unquote(f[11]),
  })
}

/** 角色：id -> { id, name, roleKey } */
const roles = new Map()
for (const row of extractValueRows(sql, 'sys_auth_role')) {
  const f = splitFields(row)
  roles.set(Number(f[0]), { id: Number(f[0]), name: unquote(f[1]), roleKey: unquote(f[2]) })
}

/** 角色-权限授权：roleId -> Set(permissionId) */
const roleGrants = new Map()
for (const row of extractValueRows(sql, 'sys_auth_role_permission')) {
  const f = splitFields(row)
  const roleId = Number(f[0])
  if (!roleGrants.has(roleId)) roleGrants.set(roleId, new Set())
  roleGrants.get(roleId).add(Number(f[1]))
}

/** 账号：id -> userName */
const users = new Map()
for (const row of extractValueRows(sql, 'sys_auth_user')) {
  const f = splitFields(row)
  users.set(Number(f[0]), unquote(f[1]))
}

/** 用户-角色：roleId -> Set(userName) */
const roleUsers = new Map()
for (const row of extractValueRows(sql, 'sys_auth_user_role')) {
  const f = splitFields(row)
  const userId = Number(f[0])
  const roleId = Number(f[1])
  if (!roleUsers.has(roleId)) roleUsers.set(roleId, new Set())
  roleUsers.get(roleId).add(users.get(userId) || '用户#' + userId)
}

/* ------------------------------------------------------------------ *
 * 二、解析接口鉴权注解（与 check-permission-doc-consistency.cjs 同口径）
 * ------------------------------------------------------------------ */
function collectJava(dir, acc = []) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name)
    if (entry.isDirectory()) collectJava(full, acc)
    else if (entry.name.endsWith('.java')) acc.push(full)
  }
  return acc
}

const classLevelRe = /@RequestMapping\(\s*(?:value\s*=\s*)?"([^"]+)"/
const methodLevelRe = /@(Post|Get|Put|Delete)Mapping\(\s*(?:value\s*=\s*)?"([^"]+)"/

function extractKeys(line) {
  if (!line.includes('@PreAuthorize')) return []
  if (!/@rbac\.has(Perm|AnyPerm)\(/.test(line)) return []
  return [...line.matchAll(/'([^']+)'/g)].map((m) => m[1])
}

/** 接口列表：[{ method, path, keys:Set, file }] */
const endpoints = []
for (const file of collectJava(BACKEND)) {
  const lines = fs.readFileSync(file, 'utf8').split(/\r?\n/)
  let classPrefix = ''
  for (const line of lines) {
    if (/@(Post|Get|Put|Delete)Mapping/.test(line)) continue
    const m = classLevelRe.exec(line)
    if (m) {
      classPrefix = m[1].replace(/\/+$/, '')
      break
    }
  }
  lines.forEach((line, i) => {
    const method = methodLevelRe.exec(line)
    if (!method) return
    const fullPath = (classPrefix + method[2]).replace(/\/{2,}/g, '/')
    const keys = new Set()
    for (let j = i; j < Math.min(i + 10, lines.length); j++) {
      extractKeys(lines[j]).forEach((k) => keys.add(k))
      if (/^\s*(public|private|protected)\s/.test(lines[j])) break
    }
    endpoints.push({
      method: method[1].toUpperCase(),
      path: fullPath,
      keys,
      file: path.relative(ROOT, file).replace(/\\/g, '/'),
    })
  })
}
endpoints.sort((a, b) => a.path.localeCompare(b.path))

/* ------------------------------------------------------------------ *
 * 三、超级角色判定（从配置读取，避免写死 admin）
 * ------------------------------------------------------------------ */
const yml = fs.readFileSync(APP_YML, 'utf8')
const adminRoleKey = (yml.match(/admin-role-key:\s*"([^"]+)"/) || [, 'admin'])[1]

/* ------------------------------------------------------------------ *
 * 四、渲染矩阵
 * ------------------------------------------------------------------ */
const publicEndpoints = endpoints.filter((e) => e.keys.size === 0)
const guardedEndpoints = endpoints.filter((e) => e.keys.size > 0)

/** 某角色是否可访问某接口：超级角色全放行；否则接口的任一权限标识需被授予 */
function canAccess(role, endpoint) {
  if (role.roleKey === adminRoleKey) return true
  const grantedKeys = new Set(
    [...(roleGrants.get(role.id) || [])].map((pid) => permissions.get(pid)?.key).filter(Boolean)
  )
  return [...endpoint.keys].some((k) => grantedKeys.has(k))
}

// 刻意不写入生成日期：否则每次运行内容都不同，--check 会"因为日期变了"而失败，
// 校验器就失去了意义（它应当只对种子/注解的变化敏感）。
const lines = []
lines.push('## 9. 角色可访问矩阵（生成内容）')
lines.push('')
lines.push(`> 本节由 \`tests/gen-role-endpoint-matrix.cjs\` 从「db/init 种子 + 后端 @PreAuthorize 注解」推导，`)
lines.push(`> **请勿手工编辑**；改动授权或接口后执行：`)
lines.push('> `node tests/gen-role-endpoint-matrix.cjs`（`--check` 只校验不写入，退出码 1 表示已过期）。')
lines.push('')
lines.push('口径说明：')
lines.push('')
lines.push('1. 超级角色（role_key = `' + adminRoleKey + '`）跳过按钮级校验，因此**可访问全部受控接口**，')
lines.push('   这是沿用旧系统的约定（见《02》4.5 内置保护规则）；')
lines.push('2. 查询类接口按**菜单权限**授权（能进页面即可查，见《02》4.4），因此菜单权限同样会出现在接口的授权来源里；')
lines.push('3. 公开接口（登录/注册/登出/取会话）不需要权限标识，单独列出；')
lines.push('4. 本矩阵描述的是**种子基线**：运行期通过界面改授权后，实际可访问范围以数据库为准')
lines.push('   （审计表 `sys_auth_audit_log` 会记录这些授权变更）。')
lines.push('')
lines.push('### 9.1 总览')
lines.push('')
lines.push('| 角色 | role_key | 账号 | 菜单数 | 按钮数 | 可访问受控接口 |')
lines.push('| ---- | ---- | ---- | ---- | ---- | ---- |')
for (const role of [...roles.values()].sort((a, b) => a.id - b.id)) {
  const grants = roleGrants.get(role.id) || new Set()
  const menuCount = [...grants].filter((pid) => permissions.get(pid)?.type === 0).length
  const buttonCount = [...grants].filter((pid) => permissions.get(pid)?.type === 1).length
  const accessCount = guardedEndpoints.filter((e) => canAccess(role, e)).length
  const accounts = [...(roleUsers.get(role.id) || [])].join(', ') || '—'
  lines.push(`| ${role.name} | \`${role.roleKey}\` | ${accounts} | ${menuCount} | ${buttonCount} | ${accessCount} |`)
}
lines.push('')
lines.push(`> 受控接口总数：${guardedEndpoints.length}；公开接口：${publicEndpoints.length}。`)
lines.push('')

let roleIndex = 2
for (const role of [...roles.values()].sort((a, b) => a.id - b.id)) {
  const grants = roleGrants.get(role.id) || new Set()
  const isSuper = role.roleKey === adminRoleKey
  lines.push(`### 9.${roleIndex++} ${role.name}（\`${role.roleKey}\`）${isSuper ? ' —— 超级角色' : ''}`)
  lines.push('')

  const menus = [...grants].map((pid) => permissions.get(pid)).filter((p) => p && p.type === 0)
  const buttons = [...grants].map((pid) => permissions.get(pid)).filter((p) => p && p.type === 1)

  lines.push('**可访问菜单**')
  lines.push('')
  if (menus.length === 0) {
    lines.push('（无）')
  } else {
    lines.push('| 菜单 | 前端路由 | permission_key |')
    lines.push('| ---- | ---- | ---- |')
    menus.forEach((p) => lines.push(`| ${p.name} | \`${p.menuUrl || '—'}\` | \`${p.key}\` |`))
  }
  lines.push('')

  lines.push('**按钮级权限**')
  lines.push('')
  if (buttons.length === 0) {
    lines.push('（无——该角色只能查看，所有写操作按钮不会渲染）')
  } else {
    lines.push('| 按钮 | permission_key |')
    lines.push('| ---- | ---- |')
    buttons.forEach((p) => lines.push(`| ${p.name} | \`${p.key}\` |`))
  }
  lines.push('')

  const accessible = guardedEndpoints.filter((e) => canAccess(role, e))
  lines.push(`**可访问受控接口（${isSuper ? '全部 ' + accessible.length + ' 个' : accessible.length + ' / ' + guardedEndpoints.length}）**`)
  lines.push('')
  if (accessible.length === 0) {
    lines.push('（无——该角色没有管理类接口权限；其可见菜单页面的数据也仅限登录即可访问的接口）')
  } else {
    lines.push('| 方法 | 接口 | 授权来源（permission_key） |')
    lines.push('| ---- | ---- | ---- |')
    accessible.forEach((e) => {
      const source = isSuper ? '超级角色放行' : [...e.keys].join(' / ')
      lines.push(`| ${e.method} | \`${e.path}\` | ${source} |`)
    })
  }
  lines.push('')
}

lines.push(`### 9.${roleIndex} 公开接口（无需权限标识）`)
lines.push('')
lines.push('| 方法 | 接口 | 说明 |')
lines.push('| ---- | ---- | ---- |')
const PUBLIC_NOTE = {
  '/auth/login': '登录（免登录白名单）',
  '/auth/register': '自助注册（生产可关闭）',
  '/auth/logout': '登出（需持有令牌，但不需要权限标识）',
  '/auth/getUserInfo': '取当前用户会话（登录即可）',
  '/auth/updateProfile': '改本人资料（本人数据，无需授权）',
  '/auth/updatePassword': '改本人密码（本人数据，无需授权）',
}
publicEndpoints.forEach((e) => {
  lines.push(`| ${e.method} | \`${e.path}\` | ${PUBLIC_NOTE[e.path] || '登录即可访问（无权限标识）'} |`)
})
lines.push('')

const section = [BEGIN, '', ...lines, END, ''].join('\n')

/* ------------------------------------------------------------------ *
 * 五、写入或校验
 * ------------------------------------------------------------------ */
const docText = fs.readFileSync(DOC, 'utf8')
const beginIdx = docText.indexOf(BEGIN)
const endIdx = docText.indexOf(END)

if (CHECK_ONLY) {
  if (beginIdx === -1 || endIdx === -1) {
    console.error('《05》中缺少角色可访问矩阵标记，请先运行 node gen-role-endpoint-matrix.cjs')
    process.exit(1)
  }
  const current = docText.slice(beginIdx, endIdx + END.length)
  if (current.trim() !== section.trim()) {
    console.error('角色可访问矩阵已过期：种子或 @PreAuthorize 发生变化，请重新运行 node gen-role-endpoint-matrix.cjs')
    process.exit(1)
  }
  console.log(
    `角色可访问矩阵已是最新：${roles.size} 个角色、${guardedEndpoints.length} 个受控接口、${publicEndpoints.length} 个公开接口`
  )
  process.exit(0)
}

let updated
if (beginIdx === -1 || endIdx === -1) {
  // 首次生成：追加到文档末尾（保证标记唯一）
  updated = docText.replace(/\s*$/, '\n\n') + section
} else {
  updated = docText.slice(0, beginIdx) + section + docText.slice(endIdx + END.length + 1)
}
fs.writeFileSync(DOC, updated, 'utf8')
console.log(
  `已写入《05》第 9 节：${roles.size} 个角色、${permissions.size} 条权限、` +
    `${guardedEndpoints.length} 个受控接口、${publicEndpoints.length} 个公开接口`
)
