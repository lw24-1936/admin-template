package com.admintem.web.service.storage;

import com.admintem.web.common.ResultCode;
import com.admintem.web.config.AdmintemProperties;
import com.admintem.web.exception.BusinessException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 图片上传校验器（本地与 MinIO 共用）。
 *
 * <p>把"能不能传"与"存到哪里"分开：策略实现只负责落盘/上传，
 * 大小、扩展名、文件头这些安全约束只写一份，避免两个实现出现规则漂移。</p>
 */
public final class ImageUploadValidator {

    private ImageUploadValidator() {
    }

    /**
     * 校验并返回小写扩展名。
     *
     * @throws BusinessException 参数不合法时抛出（对外是 400，不暴露存储细节）
     */
    public static String validate(MultipartFile file, AdmintemProperties.Upload cfg) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请选择要上传的图片");
        }
        long maxBytes = Math.max(1, cfg.getMaxSizeMb()) * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "图片不能超过 " + cfg.getMaxSizeMb() + "MB");
        }

        String extension = extensionOf(file.getOriginalFilename());
        Set<String> allowed = Arrays.stream(cfg.getAllowedExtensions().split(","))
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        if (!allowed.contains(extension)) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "仅支持 " + String.join(" / ", allowed) + " 格式的图片");
        }
        if (!looksLikeImage(file)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "文件内容不是有效的图片");
        }
        return extension;
    }

    /** 取小写扩展名；不信任客户端路径，只取最后一段 */
    static String extensionOf(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        String name = originalFilename.replace("\\", "/");
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        int dot = name.lastIndexOf('.');
        return dot >= 0 && dot < name.length() - 1
                ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    }

    /** 只读前 12 字节做文件头校验，不把整文件载入内存 */
    static boolean looksLikeImage(MultipartFile file) {
        byte[] head = new byte[12];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.read(head);
        } catch (IOException e) {
            return false;
        }
        if (read < 4) {
            return false;
        }
        boolean png = (head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G';
        boolean jpeg = (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF;
        boolean gif = head[0] == 'G' && head[1] == 'I' && head[2] == 'F' && head[3] == '8';
        boolean webp = read >= 12 && head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P';
        return png || jpeg || gif || webp;
    }

    /** 只按扩展名给 MIME：不信任客户端声明的 Content-Type，避免 PNG 被标成 SVG 等 */
    static String contentType(String extension) {
        return switch (extension) {
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            default -> "image/jpeg";
        };
    }
}
