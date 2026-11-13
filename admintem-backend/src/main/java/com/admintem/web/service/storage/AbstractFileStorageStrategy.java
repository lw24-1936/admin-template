package com.admintem.web.service.storage;

import com.admintem.web.config.AdmintemProperties;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 存储策略模板：校验、命名、URL 解析这些共性逻辑放在这里，
 * 子类只实现真正的"写入/删除"。
 */
public abstract class AbstractFileStorageStrategy implements StorageStrategy {

    protected static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyyMM");

    protected final AdmintemProperties properties;

    protected AbstractFileStorageStrategy(AdmintemProperties properties) {
        this.properties = properties;
    }

    // 注意：不能加 final。ApiLogAspect 会以 CGLIB 代理 service，final 方法绕过代理，
    // 直接读取代理对象上未注入的 properties，头像上传会 NPE。
    @Override
    public String storeAvatar(MultipartFile file) {
        // 校验只做一次，所有实现共享同一套规则
        String extension = ImageUploadValidator.validate(file, properties.getUpload());
        String key = LocalDate.now().format(MONTH) + "/"
                + UUID.randomUUID().toString().replace("-", "") + "." + extension;
        String contentType = ImageUploadValidator.contentType(extension);
        return doStore(key, file, contentType);
    }

    // 同上：deleteByUrl 也不能是 final。
    @Override
    public void deleteByUrl(String url) {
        String key = keyOf(url);
        if (key != null) {
            doDelete(key);
        }
    }

    /** 写入文件并返回可访问 URL */
    protected abstract String doStore(String key, MultipartFile file, String contentType);

    /** 删除对象；实现需自行吞掉异常并记日志 */
    protected abstract void doDelete(String key);

    /** 从 URL 解析出对象 key；不是本策略的 URL 返回 null */
    protected abstract String keyOf(String url);

    /** 防止路径穿越：key 只允许"月份/uuid.ext"这种形态 */
    protected boolean isSafeKey(String key) {
        return key != null && !key.isBlank()
                && !key.startsWith("/")
                && !key.contains("..")
                && !key.contains("\\");
    }
}
