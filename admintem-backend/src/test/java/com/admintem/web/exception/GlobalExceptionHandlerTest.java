package com.admintem.web.exception;

import com.admintem.web.common.Result;
import com.admintem.web.common.ResultCode;
import com.admintem.web.entity.dto.RoleSaveRequest;
import com.admintem.web.service.AuditLogService;
import jakarta.validation.Valid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link GlobalExceptionHandler} 异常映射单元测试（standalone MockMvc，不启动 Spring 上下文、不连库）。
 *
 * <p>锁定"客户端输入错误不落 500"这一约定：非法 JSON、参数缺失/类型不符、校验失败、
 * 唯一键冲突都应返回 HTTP 200 + 业务码 400；权限拒绝返回 403；未知异常才返回 500。
 * 这些映射是前端提示文案与告警规则的基础，一旦回退会导致监控误报。</p>
 */
@DisplayName("全局异常映射")
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    /** 测试夹具：提供触发各类异常的端点 */
    @RestController
    static class FixtureController {

        @GetMapping("/t/biz")
        public Result<Void> biz() {
            throw new BusinessException(ResultCode.PARAM_ERROR, "业务失败");
        }

        @GetMapping("/t/bizCode")
        public Result<Void> bizCode() {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }

        @GetMapping("/t/illegal")
        public Result<Void> illegal() {
            throw new IllegalArgumentException("参数不合法");
        }

        @GetMapping("/t/duplicate")
        public Result<Void> duplicate() {
            throw new DuplicateKeyException("Duplicate entry 'admin' for key 'idx_user_name'");
        }

        @GetMapping("/t/denied")
        public Result<Void> denied() {
            throw new AccessDeniedException("没有操作权限");
        }

        @GetMapping("/t/boom")
        public Result<Void> boom() {
            throw new IllegalStateException("内部故障");
        }

        @GetMapping("/t/param")
        public Result<Void> param(@RequestParam("x") String x) {
            return Result.ok();
        }

        @GetMapping("/t/num")
        public Result<Void> num(@RequestParam("n") Integer n) {
            return Result.ok();
        }

        @PostMapping("/t/body")
        public Result<Void> body(@Valid @RequestBody RoleSaveRequest request) {
            return Result.ok();
        }
    }

    @BeforeEach
    void setUp() {
        // 审计服务用 Mock：本测试只关心"异常 → 响应体"的映射，
        // 403 分支会调用审计（越权留痕），因此需要传入替身而不是真实实现。
        mockMvc = MockMvcBuilders.standaloneSetup(new FixtureController())
                .setControllerAdvice(new GlobalExceptionHandler(mock(AuditLogService.class)))
                .build();
    }

    @Test
    @DisplayName("业务异常：HTTP 200 + 异常携带的业务码与提示")
    void mapsBusinessException() throws Exception {
        mockMvc.perform(get("/t/biz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.PARAM_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value("业务失败"));
    }

    @Test
    @DisplayName("业务异常（仅预置码）：使用枚举默认提示")
    void mapsBusinessExceptionWithPresetCode() throws Exception {
        mockMvc.perform(get("/t/bizCode"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.USERNAME_EXISTS.getCode()))
                .andExpect(jsonPath("$.message").value(ResultCode.USERNAME_EXISTS.getMessage()));
    }

    @Test
    @DisplayName("非法参数：HTTP 200 + 400 业务码（不落 500）")
    void mapsIllegalArgument() throws Exception {
        mockMvc.perform(get("/t/illegal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.PARAM_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value("参数不合法"));
    }

    @Test
    @DisplayName("唯一键冲突：HTTP 200 + 400 业务码（并发重名不暴露 500）")
    void mapsDuplicateKey() throws Exception {
        mockMvc.perform(get("/t/duplicate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.PARAM_ERROR.getCode()));
    }

    @Test
    @DisplayName("必填参数缺失：HTTP 200 + 400 业务码")
    void mapsMissingParameter() throws Exception {
        mockMvc.perform(get("/t/param"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.PARAM_ERROR.getCode()));
    }

    @Test
    @DisplayName("参数类型不匹配：HTTP 200 + 400 业务码")
    void mapsTypeMismatch() throws Exception {
        mockMvc.perform(get("/t/num").param("n", "abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.PARAM_ERROR.getCode()));
    }

    @Test
    @DisplayName("请求体 JSON 非法：HTTP 200 + 400 业务码")
    void mapsUnreadableBody() throws Exception {
        mockMvc.perform(post("/t/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not-json "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.PARAM_ERROR.getCode()));
    }

    @Test
    @DisplayName("@Valid 校验失败：HTTP 200 + 400 业务码 + 字段提示")
    void mapsValidationFailure() throws Exception {
        mockMvc.perform(post("/t/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleName\":\"\",\"roleKey\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.PARAM_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("角色名称不能为空")));
    }

    @Test
    @DisplayName("权限拒绝：HTTP 403 + 403 业务码")
    void mapsAccessDenied() throws Exception {
        mockMvc.perform(get("/t/denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultCode.FORBIDDEN.getCode()));
    }

    @Test
    @DisplayName("未知异常：HTTP 500 + 500 业务码（不泄露堆栈）")
    void mapsUnknownException() throws Exception {
        mockMvc.perform(get("/t/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(ResultCode.INTERNAL_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value(ResultCode.INTERNAL_ERROR.getMessage()));
    }

    @Test
    @DisplayName("请求方法不支持：HTTP 405 + 405 业务码")
    void mapsMethodNotSupported() throws Exception {
        mockMvc.perform(post("/t/biz"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(ResultCode.METHOD_NOT_ALLOWED.getCode()));
    }
}
