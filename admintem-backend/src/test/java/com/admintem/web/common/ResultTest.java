package com.admintem.web.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 统一响应体序列化契约测试。
 *
 * <p>目的：锁死"响应体就是 {code,message,data} 三个字段"这一对外契约。
 * 背景：{@code isSuccess()} 曾作为 getter 被 Jackson 序列化成 {@code success} 字段，
 * 使真实报文与《05-接口与权限标识对照表》不一致。此测试确保不会回退。</p>
 */
class ResultTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("成功响应只序列化 code/message/data，不包含 success")
    void successJsonHasNoInternalFlag() throws Exception {
        String json = objectMapper.writeValueAsString(Result.ok("payload"));
        assertTrue(json.contains("\"code\":200"));
        assertTrue(json.contains("\"data\":\"payload\""));
        assertFalse(json.contains("success"), "success 是计算属性，不应出现在响应体中: " + json);
    }

    @Test
    @DisplayName("失败响应同样不含 success，且带业务码与提示")
    void failureJsonHasNoInternalFlag() throws Exception {
        String json = objectMapper.writeValueAsString(Result.fail(ResultCode.USERNAME_EXISTS));
        assertTrue(json.contains("\"code\":505"));
        assertTrue(json.contains("用户名已存在"));
        assertFalse(json.contains("success"));
    }
}
