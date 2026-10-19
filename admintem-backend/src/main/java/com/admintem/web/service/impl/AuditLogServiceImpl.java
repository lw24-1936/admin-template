package com.admintem.web.service.impl;

import com.admintem.web.aspect.AuditLogFactory;
import com.admintem.web.config.AuditAsyncConfig;
import com.admintem.web.config.AdmintemProperties;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.admintem.web.common.ResultCode;
import com.admintem.web.entity.SysAuthAuditLog;
import com.admintem.web.entity.dto.AuditLogPageQuery;
import com.admintem.web.entity.vo.AuditLogVO;
import com.admintem.web.entity.vo.PageResult;
import com.admintem.web.exception.BusinessException;
import com.admintem.web.mapper.SysAuthAuditLogMapper;
import com.admintem.web.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 操作审计服务实现（只追加写库）。
 *
 * <p>三条实现要点：</p>
 * <ol>
 *   <li><b>失败不外抛</b>：审计写库异常必须在最靠近持久化的一层被吸收——
 *       否则一次"表不存在/字段超长"就会让用户的正常操作失败，这违背审计的定位；</li>
 *   <li><b>入库前归一</b>：按列宽截断文本，避免超长值导致插入失败（截断而不是报错）；
 *       同时兜底 createdTime，保证时间列非空；</li>
 *   <li><b>不开启独立事务</b>：审计与业务不同事务，业务回滚时审计仍保留
 *       （例如"新增用户失败"也应留痕）；这也是不标注 {@code @Transactional} 的原因。</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    /** 时间入参/出参统一格式（前端日期控件与表格都直接用它） */
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 宽松格式：省略秒（yyyy-MM-dd HH:mm） */
    private static final DateTimeFormatter MINUTE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** 各列的字符长度上限（与 V1.0.5 建表语句一致） */
    private static final int LEN_TRACE_ID = 64;
    private static final int LEN_OPERATOR = 32;
    private static final int LEN_ACTION = 64;
    private static final int LEN_ACTION_NAME = 64;
    private static final int LEN_TARGET_TYPE = 32;
    private static final int LEN_TARGET_ID = 64;
    private static final int LEN_TARGET_NAME = 128;
    private static final int LEN_HTTP_METHOD = 8;
    private static final int LEN_REQUEST_URI = 255;
    private static final int LEN_CLIENT_IP = 64;

    private final SysAuthAuditLogMapper auditLogMapper;
    private final AdmintemProperties properties;

    @Override
    public void record(SysAuthAuditLog entry) {
        if (entry == null) {
            return;
        }
        // 开关只在这一处判断：关闭审计时，动作切面与 403 出口都不再落库（单一开关）
        if (!properties.getAudit().isEnable()) {
            return;
        }
        try {
            normalize(entry);
            auditLogMapper.insert(entry);
        } catch (Exception e) {
            log.warn("审计日志写入失败（已忽略，不影响业务）: action={}, operator={}, uri={}, err={}",
                    entry.getAction(), entry.getOperator(), entry.getRequestUri(), e.getMessage());
        }
    }

    /**
     * 异步写入：交给独立线程池，避免每个管理动作多等一次 DB round-trip。
     *
     * <p>注意：{@code @Async} 通过代理生效，本方法必须由**外部**调用（切面/处理器），
     * 类内自调用不会异步。这里也只把"落库"异步化，记录的构造仍在原线程完成——
     * 构造需要 {@code HttpServletRequest} 与 {@code SecurityContext}，跨线程会取不到。</p>
     */
    @Override
    @Async(AuditAsyncConfig.AUDIT_EXECUTOR)
    public void recordAsync(SysAuthAuditLog entry) {
        record(entry);
    }

    @Override
    public void recordAccessDenied(HttpServletRequest request, String reason) {
        // 越权留痕可单独关闭（log-denied=false）：例如被安全扫描器刷 403 时，先关它降噪
        if (!properties.getAudit().isLogDenied()) {
            return;
        }
        // 越权记录同样走异步：403 通常发生在给用户返回错误的路径上，不应再拖慢响应
        recordAsync(AuditLogFactory.accessDeniedRecord(request, reason));
    }

    @Override
    public PageResult<AuditLogVO> pageQuery(AuditLogPageQuery query) {
        // 0. 分页参数兜底（HTTP 400 由 @Valid 负责；这里防的是服务内部调用传 null）
        long pageNum = query.getPageNum() == null ? 1L : Math.max(1L, query.getPageNum());
        long pageSize = query.getPageSize() == null ? 10L : Math.min(500L, Math.max(1L, query.getPageSize()));

        // 1. 时间范围：字符串转 LocalDateTime（格式错误给出可读提示，而不是 500）
        LocalDateTime begin = parseTime(query.getBeginTime(), "开始时间", false);
        LocalDateTime end = parseTime(query.getEndTime(), "结束时间", true);
        if (begin != null && end != null && begin.isAfter(end)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "开始时间不能晚于结束时间");
        }

        // 2. 查询（SQL 侧固定 created_time DESC, id DESC；条件与索引一一对应）
        AuditLogPageQuery condition = new AuditLogPageQuery();
        condition.setOperator(trimToNull(query.getOperator()));
        condition.setAction(trimToNull(query.getAction()));
        condition.setTargetName(trimToNull(query.getTargetName()));
        condition.setResult(query.getResult());
        condition.setBeginTime(begin == null ? null : begin.format(TIME_FORMATTER));
        condition.setEndTime(end == null ? null : end.format(TIME_FORMATTER));

        IPage<SysAuthAuditLog> page = auditLogMapper.selectAuditPage(new Page<>(pageNum, pageSize), condition);
        if (page.getRecords() == null || page.getRecords().isEmpty()) {
            return new PageResult<>(Collections.emptyList(), page.getTotal(), page.getCurrent(), page.getSize());
        }

        // 3. 转 VO：时间在服务端格式化，前端直接渲染
        List<AuditLogVO> list = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public List<Map<String, Object>> queryActionDict() {
        List<Map<String, Object>> rows = auditLogMapper.selectActionDict();
        return rows == null ? Collections.emptyList() : rows;
    }

    /* ------------------------------ 查询辅助 ------------------------------ */

    /**
     * 解析时间参数。
     *
     * <p>支持三种写法，都是前端日期控件可能给出的：</p>
     * <ul>
     *   <li>{@code yyyy-MM-dd HH:mm:ss}（标准）；</li>
     *   <li>{@code yyyy-MM-dd HH:mm}（省略秒）；</li>
     *   <li>{@code yyyy-MM-dd}（只选日期；开始时间取 00:00:00，结束时间取 23:59:59）；</li>
     *   <li>ISO 的 {@code T} 分隔也能识别（把 T 归一成空格）。</li>
     * </ul>
     *
     * @param text  原始文本（为空返回 null）
     * @param label 字段中文名（用于错误提示）
     * @param isEnd 是否为结束时间（影响"只选日期"时的默认时刻）
     * @return 解析结果或 null
     */
    private LocalDateTime parseTime(String text, String label, boolean isEnd) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String value = text.trim().replace('T', ' ');
        try {
            return LocalDateTime.parse(value, TIME_FORMATTER);
        } catch (DateTimeParseException ignored) {
            // 继续尝试更宽松的格式
        }
        try {
            return LocalDateTime.parse(value, MINUTE_FORMATTER);
        } catch (DateTimeParseException ignored) {
            // 继续尝试纯日期
        }
        try {
            LocalDate date = LocalDate.parse(value);
            return isEnd ? date.atTime(23, 59, 59) : date.atStartOfDay();
        } catch (DateTimeParseException e) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    label + "格式应为 yyyy-MM-dd HH:mm:ss（也支持 yyyy-MM-dd）");
        }
    }

    /** 空白串归一为 null：避免 SQL 里出现 {@code LIKE '%'} 这类无意义条件 */
    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /** 实体 → VO（时间格式化、结果转中文） */
    private AuditLogVO toVO(SysAuthAuditLog entity) {
        AuditLogVO vo = new AuditLogVO();
        vo.setId(entity.getId());
        vo.setTraceId(entity.getTraceId());
        vo.setOperator(entity.getOperator());
        vo.setOperatorId(entity.getOperatorId());
        vo.setAction(entity.getAction());
        vo.setActionName(entity.getActionName());
        vo.setTargetType(entity.getTargetType());
        vo.setTargetId(entity.getTargetId());
        vo.setTargetName(entity.getTargetName());
        vo.setHttpMethod(entity.getHttpMethod());
        vo.setRequestUri(entity.getRequestUri());
        vo.setClientIp(entity.getClientIp());
        vo.setResult(entity.getResult());
        vo.setResultText(entity.getResult() != null && entity.getResult() == SysAuthAuditLog.RESULT_SUCCESS
                ? "成功" : "失败");
        vo.setErrorCode(entity.getErrorCode());
        vo.setErrorMsg(entity.getErrorMsg());
        vo.setCostMs(entity.getCostMs());
        vo.setCreatedTime(entity.getCreatedTime() == null ? null : entity.getCreatedTime().format(TIME_FORMATTER));
        return vo;
    }

    /* ------------------------------ 写入辅助 ------------------------------ */

    /**
     * 入库前归一：补齐时间、按列宽截断文本。
     *
     * @param entry 审计记录（原地修改）
     */
    private void normalize(SysAuthAuditLog entry) {
        if (entry.getCreatedTime() == null) {
            entry.setCreatedTime(LocalDateTime.now());
        }
        if (entry.getResult() == null) {
            entry.setResult(SysAuthAuditLog.RESULT_SUCCESS);
        }
        entry.setTraceId(truncate(entry.getTraceId(), LEN_TRACE_ID));
        entry.setOperator(truncate(entry.getOperator(), LEN_OPERATOR));
        entry.setAction(truncate(entry.getAction(), LEN_ACTION));
        entry.setActionName(truncate(entry.getActionName(), LEN_ACTION_NAME));
        entry.setTargetType(truncate(entry.getTargetType(), LEN_TARGET_TYPE));
        entry.setTargetId(truncate(entry.getTargetId(), LEN_TARGET_ID));
        entry.setTargetName(truncate(entry.getTargetName(), LEN_TARGET_NAME));
        entry.setHttpMethod(truncate(entry.getHttpMethod(), LEN_HTTP_METHOD));
        entry.setRequestUri(truncate(entry.getRequestUri(), LEN_REQUEST_URI));
        entry.setClientIp(truncate(entry.getClientIp(), LEN_CLIENT_IP));
        // 失败原因按配置截断：默认 200 字符，足够定位问题又不会写入大段堆栈
        entry.setErrorMsg(truncate(entry.getErrorMsg(), properties.getAudit().getMaxTextLength()));
    }

    /**
     * 截断文本（null 与空串原样返回）。
     *
     * @param value 原值
     * @param max   最大长度（≤0 表示不截断）
     * @return 截断后的值
     */
    private String truncate(String value, int max) {
        if (value == null || max <= 0 || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
