package com.admintem.web.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储服务（本地目录）。
 *
 * <p>本项目此前没有文件存储，头像是第一个；接口刻意做得很窄：
 * 只接受"图片类小文件"，返回对外可访问的相对 URL。这样以后换对象存储时
 * 只需替换实现，业务代码与前端都不用动。</p>
 */
public interface FileStorageService {

    /**
     * 保存头像。
     *
     * <p>安全约束（缺一不可）：</p>
     * <ol>
     *   <li>大小上限：来自配置 `admintem.upload.max-size-mb`；</li>
     *   <li>扩展名白名单：来自配置；</li>
     *   <li>**文件头校验**：真实类型必须是 PNG/JPEG/GIF/WEBP，
     *       防止把其他文件改名成 `.png` 传上来；</li>
     *   <li>文件名由服务端生成（UUID），完全不使用客户端文件名，杜绝路径穿越。</li>
     * </ol>
     *
     * @param file 上传文件
     * @return 对外可访问的相对路径，例如 `/uploads/avatar/202610/a1b2c3.png`
     */
    String storeAvatar(MultipartFile file);
}
