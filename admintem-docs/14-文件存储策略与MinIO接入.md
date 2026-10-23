# 14 · 文件存储策略（本地 / MinIO）

头像等小文件支持**本地磁盘**与 **MinIO 对象存储**两种后端，通过配置切换，默认本地。
业务层只依赖 `FileStorageService`，换存储不改 Controller、不改前端。

## 1. 架构：工厂 + 策略

```text
AuthController.uploadAvatar
        │
        ▼
FileStorageService（门面接口）
        │
        ▼
FileStorageFactory ── 按 admintem.storage.type 选择
        │
        ├── LocalFileStorageStrategy   @Component("localStorage")
        └── MinioFileStorageStrategy   @Component("minioStorage")
                │
                ▼
        AbstractFileStorageStrategy
        （校验、UUID 命名、URL 解析、路径安全）
```

| 角色 | 类 | 职责 |
| ---- | ---- | ---- |
| 门面 | `service/FileStorageService` | 业务唯一依赖：`storeAvatar` / `deleteByUrl` |
| 工厂 | `service/storage/FileStorageFactory` | 启动时解析 type，选错直接启动失败；删除时遍历全部策略 |
| 策略 | `StorageStrategy` + 两个实现 | 只负责写入/删除；校验与命名在抽象基类 |
| 校验 | `ImageUploadValidator` | 大小、扩展名、文件头、MIME，两种存储共用 |

**为什么删除要遍历全部策略**：从 local 切到 minio 后，用户旧头像可能还是另一种存储的 URL。
每个策略只认自己的 URL 前缀，遍历一遍即可跨存储清理；不匹配的直接忽略。

## 2. 配置

```yaml
admintem:
  storage:
    # local（默认）| minio
    type: ${ADMINTEM_STORAGE_TYPE:local}
    minio:
      # 9000 是 S3 API；9001 是 Web 控制台，SDK 不能连 9001
      endpoint: ${ADMINTEM_MINIO_ENDPOINT:http://127.0.0.1:9000}
      access-key: ${ADMINTEM_MINIO_ACCESS_KEY:minio}
      secret-key: ${ADMINTEM_MINIO_SECRET_KEY:}
      bucket: ${ADMINTEM_MINIO_BUCKET:admintem}
      # 浏览器访问的基础地址（不含 bucket）；留空回退 endpoint，生产建议填 CDN/公网域名
      public-url: ${ADMINTEM_MINIO_PUBLIC_URL:}
      auto-create-bucket: true
      public-read: true
```

| 配置 | 默认 | 说明 |
| ---- | ---- | ---- |
| `admintem.storage.type` | `local` | `local` / `minio`；未知值启动即报错 |
| `endpoint` | `http://127.0.0.1:9000` | **必须 9000**（S3 API），不是 9001 控制台 |
| `access-key` / `secret-key` | - | 生产用环境变量/容器 Secret，不进仓库 |
| `bucket` | `admintem` | 不存在时按 `auto-create-bucket` 创建 |
| `public-url` | 空 | 浏览器可达地址；内网 MinIO 需配公网/CDN 或反向代理 |
| `auto-create-bucket` | `true` | 首次上传时创建 |
| `public-read` | `true` | 自动设置匿名只读（头像是公开资源） |

敏感值放在 `admintem-backend/config/application-local.yml`（已在 `.gitignore`），
模板见 `config/application-local.yml.example`：

```yaml
ADMINTEM_MINIO_ENDPOINT: "http://127.0.0.1:9000"
ADMINTEM_MINIO_ACCESS_KEY: "minio"
ADMINTEM_MINIO_SECRET_KEY: "your-minio-secret"
ADMINTEM_MINIO_PUBLIC_URL: "http://127.0.0.1:9000"
```

## 3. 切换方式

```bash
# 默认：本地磁盘
java -jar target/admintem-backend.jar

# 切 MinIO：命令行覆盖（无需改代码）
java -jar target/admintem-backend.jar --admintem.storage.type=minio

# 或用环境变量（容器/CI 推荐）
set ADMINTEM_STORAGE_TYPE=minio
```

启动日志会明确当前后端：

```text
文件存储已启用: type=minio, strategy=MinioFileStorageStrategy
```

## 4. 两种策略的行为

| 项 | local | minio |
| ---- | ---- | ---- |
| 落盘位置 | `admintem.upload.avatar-dir`，按 `yyyyMM/` 分目录 | bucket 内 `yyyyMM/uuid.ext` |
| 返回 URL | `/uploads/avatar/yyyyMM/uuid.ext`（相对，前端拼 API 基址） | `public-url/bucket/yyyyMM/uuid.ext`（绝对） |
| 访问方式 | Spring 静态资源映射 `/uploads/**` | bucket 匿名只读 / CDN |
| 文件名 | 服务端 UUID，不用客户端文件名 | 同左 |
| 删除 | 校验路径必须在头像根目录内 | 只接受本服务 `public-url/bucket/` 前缀的 URL |
| 首次使用 | 自动建目录 | 自动建 bucket + 设置只读策略（可关） |

## 5. 安全设计

1. **文件头校验**：只读前 12 字节判断 PNG/JPEG/GIF/WEBP，改扩展名传脚本会被拒；
2. **MIME 只按扩展名生成**，不信任客户端 `Content-Type`；
3. **UUID 文件名** + 路径归一化 + `startsWith(root)` 双保险，杜绝路径穿越；
4. **URL 归属校验**：MinIO 删除只认本服务返回的地址前缀，防止伪造 URL 删除同 bucket 其他对象；
5. **密钥不入库**：`application-local.yml` 已被 gitignore，示例文件只放占位符；
6. **公开桶的范围**：只放头像这类公开资源；合同/证件等敏感文件不要放这个 bucket，
   应改为私有桶 + 鉴权代理下载。

## 6. 运维建议

- **public-url**：MinIO 在内网时，浏览器拿不到 `100.x` 地址，需配置公网域名、Nginx 反代或 CDN；
- **HTTPS**：前端是 HTTPS 时，图片地址也必须是 HTTPS，否则混合内容被浏览器拦截；
- **私有桶**：把 `public-read` 设为 false，改为网关代理 `/api/files/**` 或 CDN 回源，
  **不要**把预签名 URL 存进用户表（会过期）；
- **备份**：本地备份 `uploads/avatar`；MinIO 用 `mc mirror` 或桶复制；
- **容量**：头像限制 2MB（`admintem.upload.max-size-mb`），仍建议在网关限制请求体大小。

## 7. 验证记录

| 项 | 结果 |
| ---- | ---- |
| 单元测试 | 153 个全部通过（新增存储相关 9 个：校验 5、本地 2、工厂 2） |
| MinIO 上传 | 91.7KB PNG → `http://100.71.112.119:9000/admintem/202610/xxx.png` |
| MinIO 读取 | HTTP 200，`image/png`，93872B |
| 更换头像 | 新图 200；旧图变 404（对象已删除） |
| bucket | 首次上传自动创建 `admintem` 并设置匿名只读 |
| 本地存储 | 返回 `/uploads/avatar/202610/xxx.png`，经 8090 访问 200 |
| 跨策略清理 | 本地模式下上传新头像，旧 MinIO 对象被正确删除 |

## 8. 排错

| 现象 | 原因 | 处理 |
| ---- | ---- | ---- |
| 启动报“未知的文件存储类型” | `type` 拼错 | 只支持 `local` / `minio` |
| 上传报“对象存储初始化失败” | endpoint/密钥/bucket 权限 | 查 `http://host:9000/minio/health/live` 与密钥 |
| 上传成功但图片 403 | bucket 非公开、public-url 不对 | 设 `public-read=true` 或手工配桶策略/CDN |
| 图片 404 | 对象被删/URL 过期/前缀不匹配 | 看后端日志的 key 与 bucket |
| 连 9001 超时/签名错误 | 连到控制台端口 | 改成 9000 |
| HTTPS 页面图片不显示 | 混合内容 | MinIO/CDN 配 HTTPS |

## 9. 代码索引

```text
admintem-backend/src/main/java/com/admintem/web/
├── config/AdmintemProperties.java          # Storage / Minio 配置
├── service/FileStorageService.java         # 门面
└── service/storage/
    ├── StorageStrategy.java
    ├── AbstractFileStorageStrategy.java
    ├── ImageUploadValidator.java
    ├── LocalFileStorageStrategy.java
    ├── MinioFileStorageStrategy.java
    └── FileStorageFactory.java
```

前端无需改动：`resolveAssetUrl` 对 `/uploads/...` 拼 API 基址，对 `http(s)://` 原样返回。
