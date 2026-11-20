package com.admintem.web.security;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Locale;

/**
 * TOTP（RFC 6238，SHA1 / 6 位 / 30 秒）工具。
 *
 * <p>只依赖 JDK，不引入第三方 OTP 库；secret 使用 Base32 存储。
 * 校验窗口为当前时间片前后各 1 个（共 3 个），容忍客户端时钟偏差。</p>
 */
@Component
public class TotpService {

    private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final long PERIOD_SECONDS = 30L;
    private static final int DIGITS = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    /** 生成 20 字节（160 bit）随机 secret，返回无填充 Base32。 */
    public String generateSecret() {
        byte[] bytes = new byte[20];
        RANDOM.nextBytes(bytes);
        return base32Encode(bytes);
    }

    /** 生成 otpauth URI，供前端渲染二维码。 */
    public String buildOtpAuthUri(String secret, String account, String issuer) {
        String label = urlEncode((issuer == null ? "admintem" : issuer) + ":" + account);
        return "otpauth://totp/" + label
                + "?secret=" + urlEncode(secret)
                + "&issuer=" + urlEncode(issuer == null ? "admintem" : issuer)
                + "&algorithm=SHA1&digits=" + DIGITS + "&period=" + PERIOD_SECONDS;
    }

    /** 校验 6 位验证码，允许前后 1 个时间片。 */
    public boolean verify(String secret, String code) {
        return verifyAt(secret, code, System.currentTimeMillis() / 1000L);
    }

    /** 指定 epoch 秒校验（测试与时钟注入用）。 */
    boolean verifyAt(String secret, String code, long epochSeconds) {
        if (secret == null || secret.isBlank() || code == null || !code.matches("\\d{6}")) {
            return false;
        }
        try {
            byte[] key = base32Decode(secret);
            long counter = epochSeconds / PERIOD_SECONDS;
            for (long offset = -1; offset <= 1; offset++) {
                String expected = generateCode(key, counter + offset);
                if (MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),
                        code.getBytes(StandardCharsets.US_ASCII))) {
                    return true;
                }
            }
            return false;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private String generateCode(byte[] key, long counter) {
        try {
            byte[] data = new byte[8];
            long value = counter;
            for (int i = 7; i >= 0; i--) {
                data[i] = (byte) (value & 0xff);
                value >>>= 8;
            }
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);
            int offset = hash[hash.length - 1] & 0x0f;
            int binary = ((hash[offset] & 0x7f) << 24)
                    | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8)
                    | (hash[offset + 3] & 0xff);
            int otp = binary % 1_000_000;
            return String.format(Locale.ROOT, "%06d", otp);
        } catch (Exception e) {
            throw new IllegalStateException("TOTP 计算失败", e);
        }
    }

    static String base32Encode(byte[] bytes) {
        StringBuilder out = new StringBuilder((bytes.length * 8 + 4) / 5);
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : bytes) {
            buffer = (buffer << 8) | (b & 0xff);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                out.append(BASE32.charAt((buffer >> (bitsLeft - 5)) & 0x1f));
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            out.append(BASE32.charAt((buffer << (5 - bitsLeft)) & 0x1f));
        }
        return out.toString();
    }

    static byte[] base32Decode(String value) {
        String normalized = value.replace("=", "").replace(" ", "").toUpperCase(Locale.ROOT);
        int buffer = 0;
        int bitsLeft = 0;
        byte[] out = new byte[normalized.length() * 5 / 8];
        int index = 0;
        for (char c : normalized.toCharArray()) {
            int v = BASE32.indexOf(c);
            if (v < 0) {
                throw new IllegalArgumentException("非法 Base32 字符");
            }
            buffer = (buffer << 5) | v;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                out[index++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xff);
                bitsLeft -= 8;
            }
        }
        return out;
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
