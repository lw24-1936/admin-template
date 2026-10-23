package com.admintem.web.service.storage;

import com.admintem.web.common.ResultCode;
import com.admintem.web.config.AdmintemProperties;
import com.admintem.web.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 本地磁盘存储（默认策略）。
 *
 * <p>文件落在 {@code admintem.upload.avatar-dir} 下，按月份分目录；
 * URL 前缀与 {@code UploadResourceConfig} 的静态资源映射一致。</p>
 */
@Slf4j
@Component("localStorage")
public class LocalFileStorageStrategy extends AbstractFileStorageStrategy {

    public LocalFileStorageStrategy(AdmintemProperties properties) {
        super(properties);
    }

    @Override
    public String type() {
        return "local";
    }

    @Override
    protected String doStore(String key, MultipartFile file, String contentType) {
        AdmintemProperties.Upload cfg = properties.getUpload();
        Path root = root();
        Path target = root.resolve(key).normalize();
        // 双保险：UUID 文件名已排除穿越，这里再确认一次
        if (!target.startsWith(root)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "非法的文件路径");
        }
        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            log.info("头像已保存到本地: path={}, size={}B, contentType={}",
                    target, file.getSize(), contentType);
            return cfg.getUrlPrefix() + "/" + key;
        } catch (IOException e) {
            log.error("头像保存失败: root={}, key={}", root, key, e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "图片保存失败，请稍后重试");
        }
    }

    @Override
    protected void doDelete(String key) {
        Path root = root();
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) {
            return;
        }
        try {
            boolean deleted = Files.deleteIfExists(target);
            if (deleted) {
                log.info("旧头像已清理: {}", target);
            }
        } catch (IOException e) {
            log.warn("旧头像清理失败: path={}, err={}", target, e.getMessage());
        }
    }

    @Override
    protected String keyOf(String url) {
        if (url == null) {
            return null;
        }
        String prefix = properties.getUpload().getUrlPrefix() + "/";
        if (!url.startsWith(prefix)) {
            return null;
        }
        String key = url.substring(prefix.length());
        return isSafeKey(key) ? key : null;
    }

    private Path root() {
        return Paths.get(properties.getUpload().getAvatarDir()).toAbsolutePath().normalize();
    }
}
