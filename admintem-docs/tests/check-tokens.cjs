/*
 * 设计 token 一致性检查（只读，纯静态解析）
 *
 * 背景：token 层的价值来自"唯一出处"，但它有两个无声失效点——
 *   ① 引用了不存在的 token（CSS 变量写错不会报错，只会静默回退到继承值/初始值）；
 *   ② 两端 token 悄悄漂移（桌面端改深了灰阶、移动端没跟，产品观感分裂）。
 * 这两种问题靠页面评审几乎发现不了，故做成脚本门槛。
 *
 * 检查项：
 *   A. 每个 `var(--lc-*)` 引用都能在 tokens.css 中找到定义（缺失 → 失败）；
 *   B. 定义了却从未被引用的 token（未使用 → 提示，不失败，便于发现死 token）；
 *   C. 两端 token 集合与取值一致；有意不同的项必须在下方清单中登记（否则失败）。
 *
 * 运行：node check-tokens.cjs        退出码 0 = 通过
 */
const fs = require('fs')
const path = require('path')

const ROOT = path.resolve(__dirname, '..', '..')
const ENDS = [
  { name: '桌面端', dir: 'admintem-vue' },
  { name: '移动端', dir: 'admintem-mobile' },
]

/* 有意只在某一端存在的 token（越少越好，新增需说明理由） */
const DESKTOP_ONLY = {
  '--lc-font-mono': '等宽字体：桌面端用于表格数字与编号',
  '--lc-sp-8': '40px 间距：桌面端大区块留白',
  '--lc-button-h': '46px 主按钮高度：桌面端按钮规格',
  '--lc-surface-hover-soft': '更浅一档 hover 面：桌面端菜单项/顶栏用户区使用，移动端按下态用 --lc-surface-hover',
}
const MOBILE_ONLY = {
  '--lc-canvas': '宽屏下手机容器之外的画布底色',
  '--lc-frame-shadow': '手机容器的极细描边',
  '--lc-tap-min': '触摸目标最小高度（44px）',
}
/* 有意取值不同的 token（移动端字号/控件尺寸与动效时长按手感调整） */
const ALLOWED_DIFF = {
  '--lc-fs-display': '主标题：桌面 40px / 移动 27px',
  '--lc-fs-title': '卡片标题：桌面 23px / 移动 17px',
  '--lc-control-h': '输入高度：桌面 44px / 移动 46px',
  '--lc-motion-enter': '入场时长：桌面 0.4s / 移动 0.35s',
  '--lc-motion-fast': '交互反馈时长：桌面 0.18s / 移动 0.15s',
}

let failed = 0
let warned = 0
function fail(msg) {
  failed++
  console.log('  FAIL ' + msg)
}
function warn(msg) {
  warned++
  console.log('  WARN ' + msg)
}

/** 收集目录下所有前端源码文件 */
function collectFiles(dir) {
  const out = []
  const walk = (d) => {
    for (const e of fs.readdirSync(d, { withFileTypes: true })) {
      if (e.name === 'node_modules' || e.name === 'dist' || e.name.startsWith('.')) continue
      const p = path.join(d, e.name)
      if (e.isDirectory()) walk(p)
      else if (/\.(vue|css|js|mjs|ts)$/.test(e.name)) out.push(p)
    }
  }
  walk(dir)
  return out
}

/** 解析 `--lc-x: 值;` 定义（保留首次定义，模板里的重复定义视为别名覆盖） */
function parseDefs(file) {
  const css = fs.readFileSync(file, 'utf8')
  const defs = {}
  const re = /(--lc-[a-z0-9-]+)\s*:\s*([^;]+);/g
  let m
  while ((m = re.exec(css)) !== null) {
    if (!(m[1] in defs)) defs[m[1]] = m[2].trim()
  }
  return defs
}

console.log('设计 token 一致性检查')

const summary = {}
for (const end of ENDS) {
  const srcDir = path.join(ROOT, end.dir, 'src')
  const tokenFile = path.join(srcDir, 'styles', 'tokens.css')
  if (!fs.existsSync(tokenFile)) {
    fail(end.name + ' 缺少 tokens.css：' + tokenFile)
    continue
  }
  const defs = parseDefs(tokenFile)
  const files = collectFiles(srcDir)
  const used = new Set()

  for (const f of files) {
    const text = fs.readFileSync(f, 'utf8')
    // 只统计变量引用；注释中的示例（如 "--lc-xxx"）不会被 var() 包裹，天然排除
    const re = /var\(\s*(--lc-[a-z0-9-]+)\s*[,)]/g
    let m
    while ((m = re.exec(text)) !== null) used.add(m[1])
  }

  // A. 未定义引用
  const undefinedRefs = [...used].filter((t) => !(t in defs)).sort()
  if (undefinedRefs.length) {
    fail(end.name + ' 引用了未定义的 token：' + undefinedRefs.join(', '))
  } else {
    console.log('  PASS ' + end.name + '：' + used.size + ' 个 token 引用全部已定义')
  }

  // B. 未使用定义（tokens.css 自身内部引用视为使用）
  const selfUsed = new Set()
  {
    const re = /var\(\s*(--lc-[a-z0-9-]+)\s*[,)]/g
    let m
    while ((m = re.exec(fs.readFileSync(tokenFile, 'utf8'))) !== null) selfUsed.add(m[1])
  }
  const unused = Object.keys(defs)
    .filter((t) => !used.has(t) && !selfUsed.has(t))
    .sort()
  if (unused.length) {
    warn(
      end.name +
        ' 定义了但当前未被本端源码引用（两端共享色板中由另一端使用的项会落在这里，仅供审查）：' +
        unused.join(', ')
    )
  } else console.log('  PASS ' + end.name + '：无未使用的 token（定义 ' + Object.keys(defs).length + ' 个）')

  summary[end.name] = defs
}

// C. 两端一致
const [d, m] = [summary['桌面端'], summary['移动端']]
if (d && m) {
  const onlyDesktop = Object.keys(d).filter((t) => !(t in m))
  const onlyMobile = Object.keys(m).filter((t) => !(t in d))
  const badOnlyDesktop = onlyDesktop.filter((t) => !(t in DESKTOP_ONLY))
  const badOnlyMobile = onlyMobile.filter((t) => !(t in MOBILE_ONLY))
  if (badOnlyDesktop.length) fail('桌面端独有且未登记：' + badOnlyDesktop.join(', '))
  if (badOnlyMobile.length) fail('移动端独有且未登记：' + badOnlyMobile.join(', '))
  if (!badOnlyDesktop.length && !badOnlyMobile.length) {
    console.log(
      '  PASS 两端独有 token 均已登记（桌面端 ' + onlyDesktop.length + ' 个 / 移动端 ' + onlyMobile.length + ' 个）'
    )
  }

  const diff = []
  for (const t of Object.keys(d)) {
    if (!(t in m)) continue
    if (d[t].startsWith('var(') || m[t].startsWith('var(')) continue // 别名不比较字面值
    if (d[t] !== m[t]) diff.push(t + '（桌面 ' + d[t] + ' / 移动 ' + m[t] + '）')
  }
  const unregistered = diff.filter((s) => !(s.split('（')[0] in ALLOWED_DIFF))
  if (unregistered.length) {
    fail('两端取值不同且未登记：' + unregistered.join('；'))
  } else {
    console.log('  PASS 两端同名字面值差异共 ' + diff.length + ' 项，全部已登记')
  }
}

// D. 阴影类 token 必须整值使用（不能被拼进更长的 box-shadow）
//
// 背景：两端都出现过 "box-shadow: 0 20px 40px -34px var(--lc-shadow-sheet)" 这类写法。
// --lc-shadow-sheet / --lc-frame-shadow 本身就是**完整的 box-shadow 值**，
// 再拼前缀会让整条声明非法、浏览器直接丢弃 —— 表现是"阴影莫名其妙没了"，
// 既不报错也不容易发现。这里静态扫出这种拼接用法，作为永久防线。
const SHADOW_TOKENS = ['--lc-shadow-sheet', '--lc-frame-shadow']
const shadowMisuse = []
for (const end of ENDS) {
  for (const file of collectFiles(path.join(ROOT, end.dir))) {
    const text = fs.readFileSync(file, 'utf8')
    const lines = text.split(/\r?\n/)
    lines.forEach((line, index) => {
      const trimmed = line.trim()
      if (!trimmed.startsWith('box-shadow:')) return
      for (const token of SHADOW_TOKENS) {
        if (!trimmed.includes(`var(${token})`)) continue
        // 合法写法只有两种：整值 var(...)，或 var(...) 后再带颜色/inset 之外的逗号分隔段
        const wholeValue = trimmed === `box-shadow: var(${token});`
        if (!wholeValue) {
          shadowMisuse.push(`${path.relative(ROOT, file)}:${index + 1} ${trimmed}`)
        }
      }
    })
  }
}
if (shadowMisuse.length) {
  shadowMisuse.forEach((s) => fail('阴影 token 被拼接使用（整条声明会失效）：' + s))
} else {
  console.log('  PASS 阴影 token 均为整值使用（' + SHADOW_TOKENS.join(' / ') + '）')
}

console.log('\n结果：' + (failed === 0 ? '通过' : '失败 ' + failed + ' 项') + '，提示 ' + warned + ' 项')
process.exit(failed === 0 ? 0 : 1)
