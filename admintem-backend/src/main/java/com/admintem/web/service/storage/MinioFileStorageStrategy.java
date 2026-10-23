package com.admintem.web.service.storage;

import com.admintem.web.common.ResultCode;
import com.admintem.web.config.AdmintemProperties;
import com.admintem.web.exception.BusinessException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketPolicyArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * MinIO 对象存储策略。
 *
 * <p>只在 {@code admintem.storage.type=minio} 时被工厂选中；客户端懒初始化，
 * 因此默认本地存储时不会因为 MinIO 未配置而启动失败。</p>
 *
 * <p>头像属于公开资源：bucket 默认自动创建并设置匿名只读策略，
 * 返回 {@code public-url/bucket/key} 的稳定地址。若使用私有桶，请把
 * {@code public-read} 关掉并改走网关代理或 CDN，不要直接返回会过期的预签名 URL。</p>
 */
@Slf4j
@Component("minioStorage")
public class MinioFileStorageStrategy extends AbstractFileStorageStrategy {

    private volatile MinioClient client;
    private volatile boolean bucketReady;

    public MinioFileStorageStrategy(AdmintemProperties properties) {
        super(properties);
    }

    @Override
    public String type() {
        return "minio";
    }

    @Override
    protected String doStore(String key, MultipartFile file, String contentType) {
        ensureReady();
        AdmintemProperties.Minio cfg = properties.getStorage().getMinio();
        try (InputStream in = file.getInputStream()) {
            client.putObject(PutObjectArgs.builder()
                    .bucket(cfg.getBucket())
                    .object(key)
                    .stream(in, file.getSize(), -1)
                    .contentType(contentType)
                    .build());
            log.info("头像已上传到 MinIO: bucket={}, key={}, size={}B",
                    cfg.getBucket(), key, file.getSize());
            return publicBase() + "/" + key;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("MinIO 上传失败: bucket={}, key={}", cfg.getBucket(), key, e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "图片保存失败，请稍后重试");
        }
    }

    @Override
    protected void doDelete(String key) {
        try {
            MinioClient c = client();
            c.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.getStorage().getMinio().getBucket())
                    .object(key)
                    .build());
            log.info("旧头像已从 MinIO 清理: {}", key);
        } catch (Exception e) {
            log.warn("MinIO 旧头像清理失败: key={}, err={}", key, e.getMessage());
        }
    }

    @Override
    protected String keyOf(String url) {
        if (url == null) {
            return null;
        }
        // 只接受本服务返回的地址（public-url/bucket 前缀），防止伪造 URL 删到其他对象
        String base = publicBase() + "/";
        if (!url.startsWith(base)) {
            return null;
        }
        String key = url.substring(base.length());
        int query = key.indexOf('?');
        if (query >= 0) {
            key = key.substring(0, query);
        }
        return isSafeKey(key) ? key : null;
    }

    /** 首次使用时初始化客户端与 bucket，避免默认 local 时产生无谓连接 */
    private void ensureReady() {
        client();
        if (bucketReady) {
            return;
        }
        synchronized (this) {
            if (bucketReady) {
                return;
            }
            AdmintemProperties.Minio cfg = properties.getStorage().getMinio();
            try {
                boolean exists = client.bucketExists(BucketExistsArgs.builder()
                        .bucket(cfg.getBucket()).build());
                if (!exists) {
                    if (!cfg.isAutoCreateBucket()) {
                        throw new BusinessException(ResultCode.INTERNAL_ERROR,
                                "MinIO bucket 不存在: " + cfg.getBucket());
                    }
                    client.makeBucket(MakeBucketArgs.builder().bucket(cfg.getBucket()).build());
                    log.info("MinIO bucket 已创建: {}", cfg.getBucket());
                }
                if (cfg.isPublicRead()) {
                    applyPublicReadPolicy(cfg.getBucket());
                }
                bucketReady = true;
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("MinIO bucket 初始化失败: bucket={}", cfg.getBucket(), e);
                throw new BusinessException(ResultCode.INTERNAL_ERROR, "对象存储初始化失败，请检查 MinIO 配置");
            }
        }
    }

    private MinioClient client() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    AdmintemProperties.Minio cfg = properties.getStorage().getMinio();
                    if (!StringUtils.hasText(cfg.getEndpoint())) {
                        throw new BusinessException(ResultCode.INTERNAL_ERROR, "未配置 MinIO endpoint");
                    }
                    if (!StringUtils.hasText(cfg.getSecretKey())) {
                        throw new BusinessException(ResultCode.INTERNAL_ERROR, "未配置 MinIO secret-key");
                    }
                    client = MinioClient.builder()
                            .endpoint(cfg.getEndpoint())
                            .credentials(cfg.getAccessKey(), cfg.getSecretKey())
                            .build();
                }
            }
        }
        return client;
    }

    /** 头像是公开图片，给 bucket 设置只读策略；无权限时只告警，不影响上传 */
    private void applyPublicReadPolicy(String bucket) {
        String policy = """
                {
                  "Version": "2012-10-17",
                  "Statement": [{
                    "Effect": "Allow",
                    "Principal": {"AWS": ["*"]},
                    "Action": ["s3:GetObject"],
                    "Resource": ["arn:aws:s3:::%s/*"]
                  }]
                }
                """.formatted(bucket);
        try {
            client.setBucketPolicy(SetBucketPolicyArgs.builder()
                    .bucket(bucket).config(policy).build());
            log.info("MinIO bucket 已设置匿名只读策略: {}", bucket);
        } catch (Exception e) {
            log.warn("MinIO bucket 匿名只读策略设置失败（请手工设置 public-read 或配置 public-url/CDN）: {}",
                    e.getMessage());
        }
    }

    private String publicBase() {
        AdmintemProperties.Minio cfg = properties.getStorage().getMinio();
        String base = StringUtils.hasText(cfg.getPublicUrl()) ? cfg.getPublicUrl() : cfg.getEndpoint();
        return stripTrailingSlash(base) + "/" + cfg.getBucket();
    }

    private String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
