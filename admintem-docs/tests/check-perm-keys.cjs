/*
 * 权限标识一致性检查：前端 v-perm 使用的 permission_key 是否都真实存在于数据库种子
 *
 * 为什么需要它：前端把 key 写错时不会报错——按钮/入口会"静默消失"，
 * 排查成本高（本项目移动端就出现过把 userInfo:userInfo 误写成 sys:user:list 的问题，
 * 结果 4 个管理入口全部不渲染）。这里用只读脚本把这类拼写错误变成可自动发现的差异。
 *
 * 运行：node check-perm-keys.cjs      （在 admintem-docs/tests 目录）
 * 退出码：0 = 全部存在；1 = 存在前端使用但库里没有的 key
 *
 * 数据来源：
 *   - 前端：admintem-vue/src、admintem-mobile/src 下的 v-perm="'key'" / v-perm="['k1','k2']"
 *   - 库端：db/init/00_admintem_full_init.sql 与 db/changelog/*.sql 中 INSERT 的 permission_key 字面量
 */
const fs = require('fs')
const path = require('path')

const ROOT = path.resolve(__dirname, '..', '..')
const FRONTENDS = ['admintem-vue/src', 'admintem-mobile/src']
const SQL_DIRS = [path.join(ROOT, 'admintem-docs', 'db', 'init'), path.join(ROOT, 'admintem-docs', 'db', 'changelog')]

/** 递归收集指定后缀的文件 */
function collect(dir, exts, acc = []) {
  if (!fs.existsSync(dir)) return acc
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name)
    if (entry.isDirectory()) collect(full, exts, acc)
    else if (exts.some((e) => entry.name.endsWith(e))) acc.push(full)
  }
  return acc
}

/** 去掉注释：JSDoc/块注释、整行 // 注释、模板 <!-- --> 注释都会出现示例写法，避免误报 */
function stripComments(text) {
  return text
    .replace(/\/\*[\s\S]*?\*\//g, '')
    .replace(/<!--[\s\S]*?-->/g, '')
    .replace(/^\s*\/\/.*$/gm, '')
}

/* ---------------- 1. 前端使用的 key ---------------- */
const usedKeys = new Map() // key -> Set(文件相对路径)

/** 记录一次使用 */
function mark(key, file) {
  if (!key || !key.includes(':')) return // 过滤非权限标识（如变量名）
  const where = usedKeys.get(key) || new Set()
  where.add(path.relative(ROOT, file))
  usedKeys.set(key, where)
}

/*
 * 两种使用形态都要覆盖：
 *   1) 模板/指令字面量：v-perm="'a:b'"、v-perm="['a:b','c:d']"
 *   2) 程序化使用：perm: 'a:b'（如移动端首页的管理入口数组）、hasPerm('a:b')
 * 第 2 类正是"移动端把 key 写错导致入口静默消失"的形态，必须纳入检查。
 */
const permAttrRe = /v-perm="([^"]+)"/g
const quotedRe = /'([^']+)'/g
const programmaticRes = [
  /(?:perm|permissionKey|permission_key|key)\s*:\s*'([^']+)'/g,
  /hasPerm\(\s*'([^']+)'/g,
  /hasAnyPerm\(\s*([^)]+)\)/g,
]

for (const rel of FRONTENDS) {
  for (const file of collect(path.join(ROOT, rel), ['.vue', '.js'])) {
    const text = stripComments(fs.readFileSync(file, 'utf8'))
    // 形态 1：v-perm 字面量
    for (const match of text.matchAll(permAttrRe)) {
      for (const keyMatch of match[1].matchAll(quotedRe)) {
        mark(keyMatch[1], file)
      }
    }
    // 形态 2：程序化使用
    for (const re of programmaticRes) {
      for (const match of text.matchAll(re)) {
        const body = match[1]
        for (const keyMatch of body.matchAll(quotedRe)) {
          mark(keyMatch[1], file)
        }
        // hasAnyPerm('a:b','c:d') 中第一个参数不带引号包裹时（正则已含引号则上一步已覆盖）
        if (!body.includes("'") && body.includes(':')) mark(body.trim(), file)
      }
    }
  }
}

/* ---------------- 2. 数据库脚本中定义的 permission_key ---------------- */
const definedKeys = new Set()
const keyLiteralRe = /'(?!\s)([A-Za-z][A-Za-z0-9_]*:[A-Za-z][A-Za-z0-9_]*)'/g

for (const dir of SQL_DIRS) {
  for (const file of collect(dir, ['.sql'])) {
    const text = fs.readFileSync(file, 'utf8')
    for (const match of text.matchAll(keyLiteralRe)) {
      definedKeys.add(match[1])
    }
  }
}

/* ---------------- 3. 比对 ---------------- */
const problems = []
for (const [key, files] of [...usedKeys.entries()].sort()) {
  if (!definedKeys.has(key)) {
    problems.push(`[未定义] 前端使用 v-perm="${key}"（${[...files].join(', ')}），但数据库脚本中不存在该 permission_key`)
  }
}

/* ---------------- 4. 输出 ---------------- */
console.log(`前端 v-perm 使用的权限标识：${usedKeys.size} 个`)
console.log(`数据库脚本中定义的权限标识：${definedKeys.size} 个`)
if (problems.length === 0) {
  console.log('一致性检查通过：前端引用的权限标识均已定义')
  process.exit(0)
}
console.log(`发现 ${problems.length} 处问题：`)
problems.forEach((p) => console.log('  - ' + p))
process.exit(1)
