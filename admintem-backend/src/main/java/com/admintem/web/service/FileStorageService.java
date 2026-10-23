package com.admintem.web.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储服务（门面）。
 *
 * <p>具体存到哪里由 {@code admintem.storage.type} 决定（local / minio），
 * 业务层只依赖本接口。新增策略时实现 {@code StorageStrategy} 并注册 Bean 即可。</p>
 */
public interface FileStorageService {

    /**
     * 保存头像。
     *
     * <p>安全约束（两种实现共用）：</p>
     * <ol>
     *   <li>大小上限：来自配置 {@code admintem.upload.max-size-mb}；</li>
     *   <li>扩展名白名单：来自配置；</li>
     *   <li><b>文件头校验</b>：真实类型必须是 PNG/JPEG/GIF/WEBP；</li>
     *   <li>文件名由服务端生成（UUID），不使用客户端文件名。</li>
     * </ol>
     *
     * @param file 上传文件
     * @return 可访问 URL（本地为相对路径，MinIO 为绝对地址）
     */
    String storeAvatar(MultipartFile file);

    /**
     * 按 URL 删除文件（更换头像后清理旧文件）。
     *
     * <p>只删除本服务管理的 URL；非本策略地址、空值都静默忽略，失败只记日志。</p>
     */
    void deleteByUrl(String url);
}
