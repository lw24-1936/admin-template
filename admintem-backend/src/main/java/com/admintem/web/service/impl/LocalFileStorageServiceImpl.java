package com.admintem.web.service.impl;

import com.admintem.web.common.ResultCode;
import com.admintem.web.config.AdmintemProperties;
import com.admintem.web.exception.BusinessException;
import com.admintem.web.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 本地磁盘文件存储实现（头像）。
 *
 * <p>为什么选本地目录：部署简单（Docker 挂一个卷即可），不引入对象存储依赖。
 * 多实例部署时把 `admintem.upload.avatar-dir` 指到共享存储即可，接口不变。</p>
 *
 * <p><b>不信任客户端的一切输入</b>：文件名不用、扩展名只用来决定存盘后缀，
 * 真实类型靠文件头判断；路径全部由服务端拼装，客户端无法影响目录。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalFileStorageServiceImpl implements FileStorageService {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyyMM");

    private final AdmintemProperties properties;

    @Override
    public String storeAvatar(MultipartFile file) {
        AdmintemProperties.Upload cfg = properties.getUpload();

        // 1. 基本校验：空文件、大小
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请选择要上传的图片");
        }
        long maxBytes = Math.max(1, cfg.getMaxSizeMb()) * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "图片不能超过 " + cfg.getMaxSizeMb() + "MB");
        }

        // 2. 扩展名白名单（决定存盘后缀，不做安全判断）
        String extension = extensionOf(file.getOriginalFilename());
        Set<String> allowed = Arrays.stream(cfg.getAllowedExtensions().split(","))
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        if (!allowed.contains(extension)) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "仅支持 " + String.join(" / ", allowed) + " 格式的图片");
        }

        // 3. 文件头校验：真实类型必须是图片（防止把其它文件改名成 .png）
        if (!looksLikeImage(file)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "文件内容不是有效的图片");
        }

        // 4. 落盘：目录按月分桶，文件名用 UUID
        String month = LocalDate.now().format(MONTH);
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path dir = Paths.get(cfg.getAvatarDir(), month).toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(fileName).normalize();
            // 双重保险：目标必须仍在配置目录内（UUID 文件名本身已排除穿越）
            if (!target.startsWith(dir)) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "非法的文件路径");
            }
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            log.info("头像已保存: path={}, size={}B, contentType={}",
                    target, file.getSize(), file.getContentType());
            return cfg.getUrlPrefix() + "/" + month + "/" + fileName;
        } catch (IOException e) {
            // 落盘失败属于服务端问题：记 ERROR（含堆栈），对外只给通用提示
            log.error("头像保存失败: dir={}, fileName={}", dir, fileName, e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "图片保存失败，请稍后重试");
        }
    }

    /** 取小写扩展名（无扩展名返回空串；不信任客户端路径，只取最后一段） */
    private String extensionOf(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        String name = originalFilename.replace("\\", "/");
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        int dot = name.lastIndexOf('.');
        return dot >= 0 && dot < name.length() - 1 ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    }

    /**
     * 通过文件头魔数判断是否为受支持的图片类型。
     *
     * <p>只读前 12 个字节，不把整文件载入内存。</p>
     */
    private boolean looksLikeImage(MultipartFile file) {
        byte[] head = new byte[12];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.read(head);
        } catch (IOException e) {
            log.warn("读取上传文件头失败: {}", e.getMessage());
            return false;
        }
        if (read < 4) {
            return false;
        }
        // PNG: 89 50 4E 47
        boolean png = (head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G';
        // JPEG: FF D8 FF
        boolean jpeg = (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF;
        // GIF: "GIF8"
        boolean gif = head[0] == 'G' && head[1] == 'I' && head[2] == 'F' && head[3] == '8';
        // WEBP: "RIFF"...."WEBP"
        boolean webp = read >= 12 && head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P';
        return png || jpeg || gif || webp;
    }
}
