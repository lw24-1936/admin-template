package com.admintem.web.security;

import com.admintem.web.common.ResultCode;
import com.admintem.web.config.AdmintemProperties;
import com.admintem.web.entity.vo.CaptchaVO;
import com.admintem.web.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 图形验证码（数字 + 字母）：服务端渲染 PNG，Redis 保存答案，校验通过发一次性票据。
 *
 * <p>为什么用 PNG 而不是 SVG：SVG 是文本，字符会原样出现在响应里，等于把答案交给前端；
 * 这里用 AWT 直接画位图，答案只存在于服务端 Redis。</p>
 *
 * <p>易混淆字符（0/O、1/I/L）已从字符集剔除；最多 3 次尝试，票据一次性消费。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaService {

    /** 去掉 0/O/1/I/L 等易混淆字符 */
    private static final char[] CHARS = "23456789ABCDEFGHJKMNPQRSTUVWXYZ".toCharArray();
    private static final SecureRandom RND = new SecureRandom();
    private static final Font FONT = new Font("SansSerif", Font.BOLD, 28);

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;
    private final AdmintemProperties properties;

    /** 生成挑战。 */
    public CaptchaVO create() {
        AdmintemProperties.Auth.Captcha cfg = cfg();
        String code = randomCode(cfg.getLength());
        String token = UUID.randomUUID().toString().replace("-", "");
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("code", code);
        state.put("attempts", 0);
        try {
            redissonClient.getBucket(challengeKey(token))
                .set(objectMapper.writeValueAsString(state), cfg.getExpireSeconds(), TimeUnit.SECONDS);
        } catch (Exception ex) {
            throw new IllegalStateException("验证码生成失败", ex);
        }
        CaptchaVO vo = new CaptchaVO();
        vo.setToken(token);
        vo.setImage(render(code, cfg.getWidth(), cfg.getHeight()));
        vo.setWidth(cfg.getWidth());
        vo.setHeight(cfg.getHeight());
        vo.setLength(cfg.getLength());
        if (cfg.isDebug()) {
            vo.setDebugCode(code.hashCode());
            vo.setDebugText(code);
        }
        return vo;
    }

    /** 校验输入，通过则返回一次性票据。 */
    public String verify(String token, String input) {
        AdmintemProperties.Auth.Captcha cfg = cfg();
        RBucket<String> bucket = redissonClient.getBucket(challengeKey(token));
        String json = bucket.get();
        if (json == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "验证码已过期，请刷新后重试");
        }
        String expected;
        int attempts;
        try {
            Map<?, ?> state = objectMapper.readValue(json, Map.class);
            expected = String.valueOf(state.get("code"));
            attempts = ((Number) state.get("attempts")).intValue();
        } catch (Exception ex) {
            bucket.delete();
            throw new BusinessException(ResultCode.PARAM_ERROR, "验证码状态异常，请刷新");
        }
        if (expected.equalsIgnoreCase(input == null ? "" : input.trim())) {
            bucket.delete();
            String ticket = UUID.randomUUID().toString().replace("-", "");
            redissonClient.getBucket(ticketKey(ticket)).set("1", cfg.getExpireSeconds(), TimeUnit.SECONDS);
            return ticket;
        }
        attempts++;
        if (attempts >= cfg.getMaxAttempts()) {
            bucket.delete();
            throw new BusinessException(ResultCode.PARAM_ERROR, "验证码错误次数过多，请刷新");
        }
        try {
            Map<String, Object> state = new LinkedHashMap<>();
            state.put("code", expected);
            state.put("attempts", attempts);
            bucket.set(objectMapper.writeValueAsString(state), cfg.getExpireSeconds(), TimeUnit.SECONDS);
        } catch (Exception ignored) {
            // 回写失败不阻断错误提示
        }
        throw new BusinessException(ResultCode.PARAM_ERROR, "验证码不正确，请重试");
    }

    /** 登录前消费票据；验证码关闭时放行。 */
    public void assertTicket(String ticket) {
        if (!cfg().isEnabled()) {
            return;
        }
        if (ticket == null || ticket.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请先完成图形验证码");
        }
        RBucket<String> bucket = redissonClient.getBucket(ticketKey(ticket));
        if (bucket.get() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请先完成图形验证码");
        }
        bucket.delete();
    }

    private AdmintemProperties.Auth.Captcha cfg() {
        return properties.getAuth().getCaptcha();
    }

    private String challengeKey(String token) {
        return cfg().getKeyPrefix() + token;
    }

    private String ticketKey(String ticket) {
        return cfg().getTicketKeyPrefix() + ticket;
    }

    private String randomCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS[RND.nextInt(CHARS.length)]);
        }
        return sb.toString();
    }

    /** 画一张带干扰的验证码 PNG，返回 data URI。 */
    private String render(String code, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(0, 0, new Color(238, 243, 251),
                width, height, new Color(219, 230, 246)));
            g.fillRect(0, 0, width, height);

            // 干扰线
            g.setStroke(new BasicStroke(1.2f));
            for (int i = 0; i < 5; i++) {
                g.setColor(new Color(30, 58, 110, 40 + RND.nextInt(40)));
                g.drawLine(RND.nextInt(width), RND.nextInt(height), RND.nextInt(width), RND.nextInt(height));
            }
            // 噪点
            for (int i = 0; i < 60; i++) {
                g.setColor(new Color(30, 58, 110, 40 + RND.nextInt(50)));
                g.fillOval(RND.nextInt(width), RND.nextInt(height), 2, 2);
            }

            g.setFont(FONT);
            int step = width / (code.length() + 1);
            for (int i = 0; i < code.length(); i++) {
                double angle = (RND.nextDouble() - 0.5) * 0.5;
                int x = step / 2 + i * step;
                int y = height / 2 + 10 + RND.nextInt(4) - 2;
                g.setColor(i % 2 == 0 ? new Color(30, 58, 110) : new Color(47, 111, 237));
                var old = g.getTransform();
                g.rotate(angle, x, y);
                g.drawString(String.valueOf(code.charAt(i)), x, y);
                g.setTransform(old);
            }
        } finally {
            g.dispose();
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception ex) {
            throw new IllegalStateException("验证码图片生成失败", ex);
        }
    }
}
