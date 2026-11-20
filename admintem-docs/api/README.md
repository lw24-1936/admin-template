# OpenAPI 契约与类型

本目录保存后端 OpenAPI 的**可提交产物**，用于三端接口对齐与 TS 类型生成。

| 文件 | 说明 |
| ---- | ---- |
| `openapi.json` | 由后端 `/v3/api-docs` 导出的完整契约（当前 58 个 path） |
| `schema.d.ts` | 由 `openapi-typescript` 从 `openapi.json` 生成的 `paths`/`components` 类型 |

## 重新生成

先启动后端（默认 `http://localhost:8090`）：

```bash
node scripts/export-openapi.mjs
npx openapi-typescript admintem-docs/api/openapi.json -o admintem-docs/api/schema.d.ts
```

如果后端在其他地址：

```bash
OPENAPI_URL=http://127.0.0.1:8091/v3/api-docs node scripts/export-openapi.mjs
```

## 前端使用

```ts
import createClient from 'openapi-fetch'
import type { paths } from '@/../admintem-docs/api/schema'

const api = createClient<paths>({ baseUrl: import.meta.env.VITE_API_BASE_URL || '' })
const { data, error } = await api.POST('/sysConfig/paginQuery', {
  body: { pageNum: 1, pageSize: 10 },
  headers: { admintem_token: 'LAUNCH <token>' },
})
```

> 认证头仍沿用 `admintem_token: LAUNCH <accessToken>`；refreshToken 只用于 `/auth/refresh`。
> `schema.d.ts` 是生成物，修改接口后必须重新导出并提交，不要手改。
>
> **版本化别名**：请求 `/api/v1/**` 与去掉 `/api/v1` 的旧路径等价，响应带 `X-API-Version: v1`；
> 这是兼容层，不是多版本并行实现。

## 变更约定

- 接口路径/请求体/响应体变化：先改后端注解与 DTO，再重新导出；
- 同步 `admintem-docs/05-接口与权限标识对照表.md` 与角色矩阵；
- 三端 API 封装应尽量依赖生成类型，减少手写字段漂移。
