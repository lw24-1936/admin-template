package com.admintem.web.service.storage;

import com.admintem.web.config.AdmintemProperties;
import com.admintem.web.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件存储工厂：按 {@code admintem.storage.type} 选择一个策略，业务层只依赖
 * {@link FileStorageService}，不感知本地/MInIO 的差异。
 *
 * <p>启动时即解析策略：配置写错（比如 type=oss 但没有实现）会直接启动失败，
 * 而不是等到用户上传头像时才报错。</p>
 */
@Slf4j
@Service
public class FileStorageFactory implements FileStorageService {

    private final List<StorageStrategy> strategies;
    private final StorageStrategy active;

    public FileStorageFactory(List<StorageStrategy> strategies, AdmintemProperties properties) {
        this.strategies = strategies;
        String type = properties.getStorage().getType();
        if (!StringUtils.hasText(type)) {
            type = "local";
        }
        String expected = type.trim();
        this.active = strategies.stream()
                .filter(s -> s.type().equalsIgnoreCase(expected))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "未知的文件存储类型: " + expected + "，可选: " + available(strategies)));
        log.info("文件存储已启用: type={}, strategy={}", active.type(), active.getClass().getSimpleName());
    }

    private static String available(List<StorageStrategy> strategies) {
        return strategies.stream().map(StorageStrategy::type).sorted().collect(Collectors.joining(", "));
    }

    @Override
    public String storeAvatar(MultipartFile file) {
        return active.storeAvatar(file);
    }

    /**
     * 删除旧文件时遍历所有策略：每个策略只处理自己 URL 前缀/域名下的地址，
     * 这样从 local 切到 minio（或反向）后，历史头像也能被正确清理。
     */
    @Override
    public void deleteByUrl(String url) {
        for (StorageStrategy strategy : strategies) {
            try {
                strategy.deleteByUrl(url);
            } catch (Exception e) {
                log.warn("旧文件清理失败: strategy={}, url={}, err={}",
                        strategy.type(), url, e.getMessage());
            }
        }
    }

    /** 当前生效的策略标识，便于健康检查与日志排查 */
    public String activeType() {
        return active.type();
    }
}
