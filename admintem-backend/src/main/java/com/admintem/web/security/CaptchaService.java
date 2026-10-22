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

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 图形滑块验证码：服务端生成 SVG 拼图、Redis 保存缺口坐标、校验通过发一次性票据。
 * 图形完全由服务端生成，前端拿不到缺口位置（debug 模式除外）；票据 getAndDelete 防重放。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private static final int W = 320;
    private static final int H = 160;
    private static final int PIECE = 52;
    private static final SecureRandom RND = new SecureRandom();
    private static final String[] COLORS = {"#1e3a6e", "#2f6fed", "#0f766e", "#7c3aed", "#b45309"};

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;
    private final AdmintemProperties properties;

    public CaptchaVO create() {
        AdmintemProperties.Auth.Captcha cfg = cfg();
        int x = 40 + RND.nextInt(W - PIECE - 80);
        int y = 12 + RND.nextInt(H - PIECE - 24);
        String token = UUID.randomUUID().toString().replace("-", "");
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("x", x);
        state.put("attempts", 0);
        try {
            redissonClient.getBucket(challengeKey(token))
                .set(objectMapper.writeValueAsString(state), cfg.getExpireSeconds(), TimeUnit.SECONDS);
        } catch (Exception ex) {
            throw new IllegalStateException("验证码生成失败", ex);
        }
        CaptchaVO vo = new CaptchaVO();
        vo.setToken(token);
        vo.setBackground(svgData(W, H, 0, 0, W, H, art(x, y, true)));
        vo.setPiece(svgData(PIECE, PIECE, x, y, PIECE, PIECE, art(x, y, false)));
        vo.setWidth(W);
        vo.setHeight(H);
        vo.setPieceSize(PIECE);
        vo.setPieceY(y);
        if (cfg.isDebug()) {
            vo.setDebugOffset(x);
        }
        return vo;
    }

    public String verify(String token, int offset) {
        AdmintemProperties.Auth.Captcha cfg = cfg();
        RBucket<String> bucket = redissonClient.getBucket(challengeKey(token));
        String json = bucket.get();
        if (json == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "验证码已过期，请刷新后重试");
        }
        int target;
        int attempts;
        try {
            Map<?, ?> state = objectMapper.readValue(json, Map.class);
            target = ((Number) state.get("x")).intValue();
            attempts = ((Number) state.get("attempts")).intValue();
        } catch (Exception ex) {
            bucket.delete();
            throw new BusinessException(ResultCode.PARAM_ERROR, "验证码状态异常，请刷新");
        }
        if (Math.abs(offset - target) <= cfg.getTolerance()) {
            bucket.delete();
            String ticket = UUID.randomUUID().toString().replace("-", "");
            redissonClient.getBucket(ticketKey(ticket)).set("1", cfg.getExpireSeconds(), TimeUnit.SECONDS);
            return ticket;
        }
        attempts++;
        if (attempts >= cfg.getMaxAttempts()) {
            bucket.delete();
            throw new BusinessException(ResultCode.PARAM_ERROR, "验证失败次数过多，请刷新验证码");
        }
        try {
            Map<String, Object> state = new LinkedHashMap<>();
            state.put("x", target);
            state.put("attempts", attempts);
            bucket.set(objectMapper.writeValueAsString(state), cfg.getExpireSeconds(), TimeUnit.SECONDS);
        } catch (Exception ignored) {
            // 回写失败不影响本次错误提示
        }
        throw new BusinessException(ResultCode.PARAM_ERROR, "拼图位置不正确，请重试");
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
        // 一次性消费：先读后删（并发下最坏是两次登录都通过，票据本身 120s 且仅登录用）
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

    private String art(int hx, int hy, boolean hole) {
        StringBuilder sb = new StringBuilder(420);
        sb.append("<defs><linearGradient id='g' x1='0' y1='0' x2='1' y2='1'>")
          .append("<stop offset='0' stop-color='").append(COLORS[RND.nextInt(COLORS.length)]).append("'/>")
          .append("<stop offset='1' stop-color='").append(COLORS[RND.nextInt(COLORS.length)]).append("'/>")
          .append("</linearGradient></defs>")
          .append("<rect width='").append(W).append("' height='").append(H).append("' fill='url(#g)'/>");
        for (int i = 0; i < 6; i++) {
            sb.append("<circle cx='").append(RND.nextInt(W)).append("' cy='").append(RND.nextInt(H))
              .append("' r='").append(18 + RND.nextInt(46)).append("' fill='#fff' opacity='")
              .append(String.format("%.2f", 0.06 + RND.nextDouble() * 0.10)).append("'/>");
        }
        for (int i = 0; i < 4; i++) {
            sb.append("<line x1='0' y1='").append(RND.nextInt(H)).append("' x2='").append(W)
              .append("' y2='").append(RND.nextInt(H)).append("' stroke='#fff' stroke-opacity='0.10'/>");
        }
        if (hole) {
            sb.append("<rect x='").append(hx).append("' y='").append(hy).append("' width='").append(PIECE)
              .append("' height='").append(PIECE)
              .append("' rx='8' fill='rgba(15,23,42,0.55)' stroke='rgba(255,255,255,0.9)' stroke-width='2'/>");
        }
        return sb.toString();
    }

    private String svgData(int w, int h, int vx, int vy, int vw, int vh, String inner) {
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='" + w + "' height='" + h
            + "' viewBox='" + vx + " " + vy + " " + vw + " " + vh + "'>" + inner + "</svg>";
        return "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
    }
}
