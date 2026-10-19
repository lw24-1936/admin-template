package com.admintem.web.exception;

import com.admintem.web.common.Result;
import com.admintem.web.common.ResultCode;
import com.admintem.web.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 *
 * <p>职责：
 * <ul>
 *   <li>业务异常 {@link BusinessException} → HTTP 200 + 业务码；</li>
 *   <li>参数校验异常（@Valid）→ HTTP 200 + 400 业务码，字段错误以 "{@code ; }" 聚合全部提示；</li>
 *   <li>请求体/请求参数格式错误（JSON 非法、类型不匹配、必填参数缺失）→ HTTP 200 + 400 业务码，
 *       属于客户端输入问题，不落入 500 兜底；</li>
 *   <li>数据库唯一键冲突（并发重名）→ HTTP 200 + 400 业务码，避免暴露 500 内部错误；</li>
 *   <li>Spring Security 方法级权限拒绝（AccessDeniedException）→ HTTP 403；</li>
 *   <li>其余未捕获异常 → HTTP 500 并记录 ERROR 日志（不向客户端泄露堆栈）。</li>
 * </ul>
 * </p>
 *
 * <p>日志约定：客户端输入类异常只记录定位信息，不打印原始请求体，
 * 防止登录等接口的密码随解析异常写入日志。</p>
 *
 * <p>审计约定：方法级权限拒绝（@PreAuthorize 不通过）会额外写一条 ACCESS_DENIED 审计记录——
 * 这类请求不会进入业务方法，因此无法被 {@link com.admintem.web.aspect.AuditLogAspect} 捕获，
 * 但"谁尝试了无权操作"正是审计最关心的信息。</p>
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final AuditLogService auditLogService;

    /**
     * 业务异常：返回 HTTP 200，业务状态码由异常携带。
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 方法参数校验失败（@RequestBody + @Valid）。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        return buildParamError(e.getBindingResult().getFieldErrors()
                .stream().map(FieldError::getDefaultMessage).collect(Collectors.joining("; ")));
    }

    /**
     * 表单绑定校验失败。
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        return buildParamError(e.getBindingResult().getFieldErrors()
                .stream().map(FieldError::getDefaultMessage).collect(Collectors.joining("; ")));
    }

    /**
     * 兜底非法参数。
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("参数非法: {}", e.getMessage());
        return Result.fail(ResultCode.PARAM_ERROR, e.getMessage());
    }

    /**
     * 请求体反序列化失败（JSON 语法错误、字段类型不匹配等）。
     *
     * <p>属于客户端输入问题：修复前会落入 {@link #handleException} 兜底并返回 HTTP 500，
     * 使前端把「传错 JSON」误判为服务端故障。</p>
     *
     * <p>此处刻意不打印异常原文：Jackson 的异常消息会附带请求体片段，
     * 登录等接口可能因此把密码写入日志。</p>
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败（JSON 格式或字段类型不匹配），已按参数错误返回");
        return Result.fail(ResultCode.PARAM_ERROR, "请求体格式不正确，请检查 JSON 格式与字段类型");
    }

    /**
     * 请求参数类型不匹配（如把 boolean 参数传成任意字符串）。
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("请求参数类型不匹配: name={}, value={}", e.getName(), e.getValue());
        return Result.fail(ResultCode.PARAM_ERROR, "参数 " + e.getName() + " 类型不正确");
    }

    /**
     * 必填请求参数缺失。
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParameter(MissingServletRequestParameterException e) {
        log.warn("缺少必填请求参数: {}", e.getParameterName());
        return Result.fail(ResultCode.PARAM_ERROR, "缺少必填参数：" + e.getParameterName());
    }

    /**
     * 数据库唯一键冲突。
     *
     * <p>重名/重标识已由业务层先查后写（count + insert）拦截，
     * 但并发提交时仍会撞唯一索引；此时若不单独处理会返回 HTTP 500
     * 「服务器内部错误」，用户完全不知道是重名导致。</p>
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicateKey(DuplicateKeyException e) {
        log.error("数据库唯一性约束冲突: {}", e.getMostSpecificCause().getMessage(), e);
        return Result.fail(ResultCode.PARAM_ERROR, "数据已存在（唯一性冲突），请检查名称/标识是否重复");
    }

    /**
     * Spring Security 方法级权限校验失败（@PreAuthorize 不通过时抛出）。
     * 返回 HTTP 403 + 统一错误体，前端据此提示"无权限"。
     *
     * <p>同时写一条越权审计（动作码 ACCESS_DENIED）：记录操作人、请求路径、IP 与 traceId。</p>
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccessDenied(AccessDeniedException e, HttpServletRequest request) {
        log.warn("访问被拒绝: {}", e.getMessage());
        auditLogService.recordAccessDenied(request, e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Result.fail(ResultCode.FORBIDDEN));
    }

    /**
     * 静态资源/接口不存在。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNotFound(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Result.fail(ResultCode.NOT_FOUND));
    }

    /**
     * HTTP 方法不支持（如对 GET 接口发起 POST 请求）。
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持: {}", e.getMessage());
        // 使用统一枚举常量而非裸码 405，保持"码值集中定义"的约定
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Result.fail(ResultCode.METHOD_NOT_ALLOWED.getCode(),
                        ResultCode.METHOD_NOT_ALLOWED.getMessage() + "：" + e.getMessage()));
    }

    /**
     * 未知异常兜底：记录完整堆栈，返回 HTTP 500。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        log.error("系统异常: ", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(ResultCode.INTERNAL_ERROR));
    }

    /* ------------------------------ 私有方法 ------------------------------ */

    /** 构造参数错误响应 */
    private Result<Void> buildParamError(String message) {
        log.warn("参数校验失败: {}", message);
        return Result.fail(ResultCode.PARAM_ERROR, message);
    }
}
