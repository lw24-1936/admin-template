#!/usr/bin/env node
/**
 * Vue SFC 结构顺序检查：template → script → style。
 *
 * 只检查四个前端工程 src 下的 .vue；App.vue 等没有 template 的文件也允许，
 * 但已有块之间必须保持上述顺序。脚本不依赖 @vue/compiler-sfc，按顶层块解析，
 * 避免把模板里的 <template v-if> 误判成 SFC 块。
 */
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const roots = [
  'admintem-vue/src',
  'admintem-mobile/src',
  'admintem-uniapp/src',
  'admintem-site/src',
]
const EXPECTED = ['template', 'script', 'style']

function walk(dir, out) {
  if (!fs.existsSync(dir)) return
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name)
    if (entry.isDirectory()) {
      if (entry.name === 'node_modules' || entry.name === 'dist') continue
      walk(full, out)
    } else if (entry.isFile() && entry.name.endsWith('.vue')) {
      out.push(full)
    }
  }
}

/** 返回顶层 SFC 块的类型顺序。 */
function blockOrder(source) {
  const lines = source.split(/\r?\n/)
  const blocks = []
  let i = 0
  while (i < lines.length) {
    const line = lines[i]
    const open = line.match(/^\s*<(template|script|style)(?:\s|>)/)
    if (!open) {
      i += 1
      continue
    }
    const type = open[1]
    if (type === 'template') {
      let depth = 0
      let j = i
      while (j < lines.length) {
        depth += (lines[j].match(/<template\b/g) || []).length
        depth -= (lines[j].match(/<\/template>/g) || []).length
        if (depth === 0) break
        j += 1
      }
      blocks.push(type)
      i = j + 1
      continue
    }
    const close = type === 'script' ? /^\s*<\/script>/ : /^\s*<\/style>/
    let j = i + 1
    while (j < lines.length && !close.test(lines[j])) j += 1
    blocks.push(type)
    i = j + 1
  }
  return blocks
}

const files = []
for (const rel of roots) walk(path.join(root, rel), files)
const errors = []
for (const file of files) {
  const source = fs.readFileSync(file, 'utf8')
  const order = blockOrder(source)
  const filtered = order.filter((type) => EXPECTED.includes(type))
  // 多个 <style> 块合法；比较前把连续 style 折叠成一个。
  const compressed = filtered.filter((type, index) => type !== 'style' || filtered[index - 1] !== 'style')
  const expected = EXPECTED.filter((type) => compressed.includes(type))
  if (compressed.join(',') !== expected.join(',')) {
    errors.push({
      file: path.relative(root, file).replace(/\\/g, '/'),
      actual: filtered.join(' → ') || '(none)',
      expected: expected.join(' → '),
    })
  }
}

if (errors.length) {
  console.error('SFC 顺序检查失败：必须为 template → script → style')
  for (const error of errors) {
    console.error(`  ${error.file}: ${error.actual}（期望 ${error.expected}）`)
  }
  process.exit(1)
}
console.log(`SFC 顺序检查通过：${files.length} 个 .vue 文件均为 template → script → style`)
