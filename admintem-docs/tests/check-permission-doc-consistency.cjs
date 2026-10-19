/*
 * 文档一致性检查：比对《05-接口与权限标识对照表》与后端 @PreAuthorize 注解
 *
 * 目的：文档里写的"接口 -> 权限标识"必须与代码注解一致；人工评审容易漂移，
 * 这里做一次**只读**的脚本化比对，作为发布前卡点。
 *
 * 运行：node check-permission-doc-consistency.cjs   （在 admintem-docs/tests 目录）
 * 退出码：0 = 无差异；1 = 存在差异
 *
 * 比对口径：
 *   1. 代码侧：拼接"类级 @RequestMapping 前缀 + 方法级 @PostMapping 路径"，
 *      取同注解块内的 @PreAuthorize("@rbac.hasPerm('key')")（支持多个 key）；   2. 文档侧：只取带 HTTP 方法列（GET/POST/...）的表格行作为"接口行"，
 *      菜单表（无方法列）不参与接口比对；
 *   3. 差异判定：
 *      - 代码有注解但文档无该接口行            -> 报错
 *      - 文档接口行有注解但未出现该权限标识    -> 报错
 *      - 代码无注解但文档该行声称了权限标识    -> 报错（说明文档写了并不存在的鉴权）
 *      - 代码无注解且文档标注为公开/登录即可   -> 通过（属于设计内，如 /auth/login）
 */
const fs = require('fs')
const path = require('path')

const ROOT = path.resolve(__dirname, '..', '..')
const BACKEND = path.join(ROOT, 'admintem-backend', 'src', 'main', 'java')
const DOC = path.join(ROOT, 'admintem-docs', '05-接口与权限标识对照表.md')

/** 递归收集 .java 文件 */
function collectJava(dir, acc = []) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name)
    if (entry.isDirectory()) collectJava(full, acc)
    else if (entry.name.endsWith('.java')) acc.push(full)
  }
  return acc
}

/* ---------------- 1. 代码侧：接口路径 -> 权限标识集合 ---------------- */
const classLevelRe = /@RequestMapping\(\s*(?:value\s*=\s*)?"([^"]+)"/
const methodLevelRe = /@(Post|Get|Put|Delete)Mapping\(\s*(?:value\s*=\s*)?"([^"]+)"/
/**
 * 提取一行 @PreAuthorize 中的权限标识。
 * 支持两种写法（本项目均在用）：
 *   @PreAuthorize("@rbac.hasPerm('userInfo:userInfo')")
 *   @PreAuthorize("@rbac.hasAnyPerm('userInfo:userInfo', 'userInfo:roleInfo')")
 * 返回该行出现的所有权限标识（无则空数组）。
 */
function extractKeys(line) {
  if (!line.includes('@PreAuthorize')) return []
  if (!/@rbac\.has(Perm|AnyPerm)\(/.test(line)) return []
  return [...line.matchAll(/'([^']+)'/g)].map((m) => m[1])
}

const codeMappings = new Map() // 完整路径 -> { file, keys:Set }
for (const file of collectJava(BACKEND)) {
  const lines = fs.readFileSync(file, 'utf8').split(/\r?\n/)
  // 类级前缀（取文件中第一个类级 @RequestMapping，且不含方法级注解）
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
    const record = codeMappings.get(fullPath) || { file: path.relative(ROOT, file), keys: new Set() }
    keys.forEach((k) => record.keys.add(k))
    codeMappings.set(fullPath, record)
  })
}

/* ---------------- 2. 文档侧：接口行（含 HTTP 方法列） ---------------- */
const docText = fs.readFileSync(DOC, 'utf8')
const docRows = []
for (const line of docText.split(/\r?\n/)) {
  if (!line.trim().startsWith('|')) continue
  const cells = line.split('|').map((c) => c.trim())
  const methodCell = cells.find((c) => /^(GET|POST|PUT|DELETE)$/i.test(c))
  const routeCell = cells.find((c) => /^\/[A-Za-z]/.test(c))
  if (!methodCell || !routeCell) continue
  docRows.push({ route: routeCell, raw: line })
}

/* ---------------- 3. 比对 ---------------- */
const problems = []
const docRouteSet = new Set(docRows.map((r) => r.route))

// 3.1 代码有权限注解，文档必须有对应接口行，且该行要出现注解中的标识
for (const [routePath, record] of codeMappings) {
  if (record.keys.size === 0) continue
  const hit = docRows.find((row) => row.route === routePath)
  if (!hit) {
    problems.push(`[文档缺行] 接口 ${routePath} 有 @PreAuthorize（${[...record.keys].join(', ')}），但 05 文档未登记`)
    continue
  }
  for (const key of record.keys) {
    if (!hit.raw.includes(key)) {
      problems.push(`[标识不一致] 接口 ${routePath} 注解为 ${key}，05 文档该行未出现该标识`)
    }
  }
}

// 3.2 文档登记了接口但代码无该方法映射
for (const row of docRows) {
  const exists = codeMappings.has(row.route)
  if (!exists) {
    problems.push(`[代码缺接口] 05 文档登记了 ${row.route}，但代码中未找到对应方法映射`)
    continue
  }
  const keys = codeMappings.get(row.route).keys
  // 3.3 文档声称有权限标识（形如 a:b），而代码没有 @PreAuthorize
  const claimedKey = row.raw.match(/[A-Za-z][A-Za-z0-9]*:[A-Za-z][A-Za-z0-9]*/)
  if (keys.size === 0 && claimedKey) {
    problems.push(`[文档夸大] ${row.route} 在文档中声称需要权限 ${claimedKey[0]}，但代码无 @PreAuthorize`)
  }
}

/* ---------------- 4. 输出 ---------------- */
console.log(`代码侧带权限注解的接口：${[...codeMappings.values()].filter((r) => r.keys.size > 0).length} 个`)
console.log(`文档侧接口行：${docRows.length} 条（菜单路由行已排除）`)
if (problems.length === 0) {
  console.log('一致性检查通过：05 文档与 @PreAuthorize 完全对应')
  process.exit(0)
}
console.log(`发现 ${problems.length} 处差异：`)
problems.forEach((p) => console.log('  - ' + p))
process.exit(1)
