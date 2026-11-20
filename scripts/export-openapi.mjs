/**
 * 从正在运行的后端导出 OpenAPI 契约。
 *
 * 用法：
 *   node scripts/export-openapi.mjs
 *   OPENAPI_URL=http://localhost:8090/v3/api-docs node scripts/export-openapi.mjs
 *
 * 约定：先启动后端（或 docker compose up backend），再导出并提交生成的
 * admintem-docs/api/openapi.json；类型文件用 openapi-typescript 重新生成：
 *   npx openapi-typescript admintem-docs/api/openapi.json -o admintem-docs/api/schema.d.ts
 */
import { writeFile } from 'node:fs/promises'
import { resolve } from 'node:path'

const url = process.env.OPENAPI_URL || 'http://localhost:8090/v3/api-docs'
const target = resolve('admintem-docs/api/openapi.json')

const response = await fetch(url)
if (!response.ok) {
  throw new Error(`OpenAPI 导出失败: ${response.status} ${response.statusText}`)
}
const document = await response.json()
await writeFile(target, JSON.stringify(document, null, 2) + '\n', 'utf8')
console.log(`已写入 ${target}: ${Object.keys(document.paths || {}).length} paths`)
