package com.admintem.web.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.Reader;
import java.util.regex.Pattern;

/**
 * 方法日志脱敏工具。
 *
 * <p>日志要"能定位问题"，但不能把密码、令牌写进文件。这里统一做三件事：
 * 敏感字段替换、超长内容截断、不可序列化对象降级描述。</p>
 */
public final class LogSanitizer {

    /** JSON 里的敏感字段：只替换字符串值，数字状态码（如 code=200）不动 */
    private static final Pattern SENSITIVE_JSON = Pattern.compile(
            "(\"(?:password|oldPassword|newPassword|confirmPassword|token|accessToken|refreshToken"
                    + "|authorization|secret|secretKey|captcha|captchaCode|verifyCode)\"\\s*:\\s*)\"(?:[^\"\\\\]|\\\\.)*\"",
            Pattern.CASE_INSENSITIVE);

    /** 非 JSON 场景（toString 降级）的敏感键值对 */
    private static final Pattern SENSITIVE_TEXT = Pattern.compile(
            "(?i)(password|oldPassword|newPassword|confirmPassword|token|accessToken|refreshToken"
                    + "|authorization|secret|secretKey|captcha|verifyCode)\\s*=\\s*(?!Bearer\\b)[^,;\\s}]+");

    /** Authorization 头/日志里的 Bearer 令牌 */
    private static final Pattern BEARER = Pattern.compile("(?i)(Bearer\\s+)[A-Za-z0-9._-]+");

    private LogSanitizer() {
    }

    /** 脱敏：密码/令牌等统一替换为 ***，其余内容保持原样 */
    public static String mask(String text) {
        if (text == null) {
            return null;
        }
        // 先处理 Bearer，避免 Authorization=Bearer <token> 的值被键值规则截成 "Bearer"
        String masked = BEARER.matcher(text).replaceAll("$1***");
        masked = SENSITIVE_JSON.matcher(masked).replaceAll("$1\"***\"");
        return SENSITIVE_TEXT.matcher(masked).replaceAll("$1=***");
    }

    /** 截断：避免把整页列表/大字段写进日志 */
    public static String truncate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        int limit = Math.max(32, maxLength);
        return text.length() <= limit ? text : text.substring(0, limit) + "...(" + text.length() + " chars)";
    }

    /**
     * 把任意参数/返回值转成可读文本。
     *
     * <p>文件、流、Servlet 对象不展开（体积大且无意义）；
     * 其余对象走 Jackson，失败时降级为 toString，并统一脱敏、截断。</p>
     */
    public static String describe(Object value, ObjectMapper objectMapper, int maxLength) {
        if (value == null) {
            return "null";
        }
        if (value instanceof MultipartFile file) {
            return "MultipartFile(name=" + file.getOriginalFilename()
                    + ", size=" + file.getSize() + "B, contentType=" + file.getContentType() + ")";
        }
        if (value instanceof byte[] bytes) {
            return "byte[" + bytes.length + "]";
        }
        if (value instanceof InputStream) {
            return value.getClass().getSimpleName();
        }
        if (value instanceof Reader) {
            return value.getClass().getSimpleName();
        }
        if (value instanceof jakarta.servlet.ServletRequest || value instanceof jakarta.servlet.ServletResponse) {
            return value.getClass().getSimpleName();
        }
        if (value instanceof Throwable throwable) {
            return throwable.getClass().getSimpleName() + ": " + throwable.getMessage();
        }

        String text;
        try {
            text = objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            text = String.valueOf(value);
        }
        return truncate(mask(text), maxLength);
    }
}
