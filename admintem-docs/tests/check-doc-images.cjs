/*
 * 文档配图与链接检查（只读）
 *
 * 目的：文档里的截图/相对链接一旦失效，读者看到的是破图而作者不易察觉。
 * 本脚本扫描仓库内所有 Markdown 的相对图片引用与文档互链，逐条验证目标是否存在。
 *
 * 运行：node check-doc-images.cjs      （在 admintem-docs/tests 目录）
 * 退出码：0 = 全部存在；1 = 存在失效引用（并打印具体文件与行号）
 */
const fs = require('fs')
const path = require('path')

const ROOT = path.resolve(__dirname, '..', '..')

/** 需要检查的 Markdown：仓库根、docs、两端 README */
const TARGETS = [
  path.join(ROOT, 'README.md'),
  path.join(ROOT, 'admintem-vue', 'README.md'),
  path.join(ROOT, 'admintem-mobile', 'README.md'),
  path.join(ROOT, 'admintem-backend', 'README.md'),
  ...fs.readdirSync(path.join(ROOT, 'admintem-docs'))
    .filter((f) => f.endsWith('.md'))
    .map((f) => path.join(ROOT, 'admintem-docs', f)),
]

/** 匹配 Markdown 图片 ![alt](target) 与链接 [text](target) */
const refRe = /!?\[[^\]]*\]\(([^)\s]+)\)/g

const problems = []
let checked = 0

for (const file of TARGETS) {
  if (!fs.existsSync(file)) {
    problems.push(`[文档缺失] ${path.relative(ROOT, file)} 不存在`)
    continue
  }
  const lines = fs.readFileSync(file, 'utf8').split(/\r?\n/)
  lines.forEach((line, idx) => {
    for (const match of line.matchAll(refRe)) {
      const target = match[1]
      // 跳过外链、锚点、邮件
      if (/^(https?:|mailto:|#)/.test(target)) continue
      // 去掉锚点部分
      const clean = target.split('#')[0]
      if (!clean) continue
      checked++
      const resolved = path.resolve(path.dirname(file), decodeURIComponent(clean))
      if (!fs.existsSync(resolved)) {
        problems.push(`[引用失效] ${path.relative(ROOT, file)}:${idx + 1} -> ${target}`)
      }
    }
  })
}

/* ------------------------------ 输出 ------------------------------ */
const imageDir = path.join(ROOT, 'admintem-docs', 'images')
const imageCount = fs.existsSync(imageDir)
  ? fs.readdirSync(imageDir).filter((f) => f.endsWith('.png')).length
  : 0

console.log(`检查 Markdown 文件 ${TARGETS.length} 个，相对引用 ${checked} 处`)
console.log(`admintem-docs/images 下截图 ${imageCount} 张`)
if (problems.length === 0) {
  console.log('文档配图与相对链接检查通过：全部指向存在的文件')
  process.exit(0)
}
console.log(`发现 ${problems.length} 处问题：`)
problems.forEach((p) => console.log('  - ' + p))
process.exit(1)
