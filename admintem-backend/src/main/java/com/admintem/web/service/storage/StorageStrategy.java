package com.admintem.web.service.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储策略。
 *
 * <p>只做"存"与"删"，不关心业务；由 {@code FileStorageFactory} 按配置选一个实现。
 * 新增存储（OSS、S3、FTP）时实现本接口并注册为 Spring Bean 即可，业务层不用改。</p>
 */
public interface StorageStrategy {

    /** 策略标识，与 {@code admintem.storage.type} 对应：local / minio */
    String type();

    /**
     * 保存头像。
     *
     * @return 浏览器可访问的 URL（本地是 /uploads/... 相对路径，MinIO 是绝对地址）
     */
    String storeAvatar(MultipartFile file);

    /**
     * 保存文件中心的通用附件。
     *
     * @param file        上传文件
     * @param extension   已校验的小写扩展名（不含点）
     * @param contentType 服务端判定的 MIME 类型
     * @return 可访问 URL
     */
    String storeFile(MultipartFile file, String extension, String contentType);

    /**
     * 按 URL 删除文件；不是本策略管理的 URL 时静默忽略。
     * 用于更换头像后清理旧文件，失败只记日志，不影响主流程。
     */
    void deleteByUrl(String url);
}
