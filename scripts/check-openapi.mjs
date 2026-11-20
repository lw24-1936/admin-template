/**
 * OpenAPI 契约基本一致性检查（不依赖后端）。
 *
 * 校验：JSON 可解析、paths 非空、每个 operation 有 summary/tags、
 * admintem_token 安全方案存在。接口实现与生成物是否漂移，仍需在改接口后
 * 重新执行 `node scripts/export-openapi.mjs` 并审查 diff。
 */
import { readFile } from 'node:fs/promises'
import { resolve } from 'node:path'

const file = resolve('admintem-docs/api/openapi.json')
const document = JSON.parse(await readFile(file, 'utf8'))
const paths = document.paths || {}
if (Object.keys(paths).length === 0) throw new Error('OpenAPI 没有 paths')
if (!document.components?.securitySchemes?.admintem_token) {
  throw new Error('OpenAPI 缺少 admintem_token 安全方案')
}
const problems = []
for (const [path, item] of Object.entries(paths)) {
  for (const [method, operation] of Object.entries(item)) {
    if (method === 'parameters') continue
    if (!operation.summary) problems.push(`${method.toUpperCase()} ${path} 缺少 summary`)
    if (!operation.tags?.length) problems.push(`${method.toUpperCase()} ${path} 缺少 tags`)
  }
}
if (problems.length) {
  throw new Error(`OpenAPI 契约检查失败：\n${problems.join('\n')}`)
}
console.log(`OpenAPI 契约检查通过：${Object.keys(paths).length} paths，${Object.keys(document.components.schemas || {}).length} schemas`)
