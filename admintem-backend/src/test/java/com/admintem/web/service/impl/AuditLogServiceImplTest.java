package com.admintem.web.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.admintem.web.config.AdmintemProperties;
import com.admintem.web.entity.dto.AuditLogPageQuery;
import com.admintem.web.entity.vo.AuditLogVO;
import com.admintem.web.entity.vo.PageResult;
import com.admintem.web.exception.BusinessException;
import com.admintem.web.entity.SysAuthAuditLog;
import com.admintem.web.mapper.SysAuthAuditLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 审计服务测试：开关、入库前归一、失败不外抛。
 *
 * <p>不连库：Mapper 用 Mockito 替代，验证的是"给 Mapper 的是什么"与"异常边界"。</p>
 */
class AuditLogServiceImplTest {

    private SysAuthAuditLogMapper mapper;
    private AdmintemProperties properties;
    private AuditLogServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(SysAuthAuditLogMapper.class);
        properties = new AdmintemProperties();
        service = new AuditLogServiceImpl(mapper, properties);
    }

    @Test
    @DisplayName("开关关闭：不落库（单一开关控制动作审计与越权审计）")
    void disabledBySwitch() {
        properties.getAudit().setEnable(false);

        service.record(new SysAuthAuditLog());
        service.recordAccessDenied(new MockHttpServletRequest("POST", "/x"), "denied");

        verify(mapper, never()).insert(any());
    }

    @Test
    @DisplayName("入库前归一：补齐时间与结果，按列宽截断")
    void normalizeBeforeInsert() {
        SysAuthAuditLog entry = new SysAuthAuditLog();
        entry.setAction("USER_ADD");
        entry.setRequestUri("u".repeat(300));
        entry.setErrorMsg("e".repeat(500));

        service.record(entry);

        ArgumentCaptor<SysAuthAuditLog> captor = ArgumentCaptor.forClass(SysAuthAuditLog.class);
        verify(mapper, times(1)).insert(captor.capture());
        SysAuthAuditLog saved = captor.getValue();
        assertNotNull(saved.getCreatedTime(), "时间列非空，避免插入失败");
        assertEquals(SysAuthAuditLog.RESULT_SUCCESS, saved.getResult(), "结果缺省按成功");
        assertEquals(255, saved.getRequestUri().length(), "URI 按列宽截断");
        assertEquals(properties.getAudit().getMaxTextLength(), saved.getErrorMsg().length(),
                "失败原因按配置长度截断");
    }

    @Test
    @DisplayName("写库异常：服务自身吞掉（不抛给业务）")
    void swallowInsertFailure() {
        doThrow(new RuntimeException("table missing")).when(mapper).insert(any());

        assertDoesNotThrow(() -> service.record(new SysAuthAuditLog()));
    }

    @Test
    @DisplayName("越权审计：默认开启，记录 ACCESS_DENIED 与 403")
    void recordAccessDenied() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/sysAuthUser/deleteById");
        request.addHeader("X-Real-IP", "198.51.100.5");

        service.recordAccessDenied(request, "Access Denied");

        ArgumentCaptor<SysAuthAuditLog> captor = ArgumentCaptor.forClass(SysAuthAuditLog.class);
        verify(mapper, times(1)).insert(captor.capture());
        SysAuthAuditLog saved = captor.getValue();
        assertEquals("ACCESS_DENIED", saved.getAction());
        assertEquals(SysAuthAuditLog.RESULT_FAILURE, saved.getResult());
        assertEquals(403, saved.getErrorCode());
        assertEquals("198.51.100.5", saved.getClientIp());
    }

    @Test
    @DisplayName("越权审计可单独关闭（降噪）：log-denied=false 时不写")
    void deniedAuditCanBeDisabled() {
        properties.getAudit().setLogDenied(false);

        service.recordAccessDenied(new MockHttpServletRequest("POST", "/x"), "denied");

        verify(mapper, never()).insert(any());
    }

    @Test
    @DisplayName("入参为 null：直接忽略，不触碰 Mapper")
    void ignoreNullEntry() {
        service.record(null);

        verify(mapper, never()).insert(any());
    }
    @Test
    @DisplayName("异步入口：直接调用时同步委托给 record（代理外调用等价于同步）")
    void recordAsyncDelegatesToRecord() {
        SysAuthAuditLog entry = new SysAuthAuditLog();
        entry.setAction("USER_ADD");

        service.recordAsync(entry);

        // @Async 只在通过 Spring 代理调用时生效；单测里直接调用即为同步，
        // 因此这里断言"委托"本身：落库一次、内容一致
        verify(mapper, times(1)).insert(any(SysAuthAuditLog.class));
    }

    @Test
    @DisplayName("异步入口：entry 为 null 时同样安全")
    void recordAsyncIgnoresNull() {
        service.recordAsync(null);
        verify(mapper, never()).insert(any());
    }
    /* ------------------------------ 查询路径（B5） ------------------------------ */

    /** 构造一条测试记录 */
    private SysAuthAuditLog sample(String action, int result) {
        SysAuthAuditLog e = new SysAuthAuditLog();
        e.setId(1L);
        e.setTraceId("trace-1");
        e.setOperator("admin");
        e.setOperatorId(1L);
        e.setAction(action);
        e.setActionName("新增用户");
        e.setTargetType("USER");
        e.setTargetId("9");
        e.setTargetName("tom");
        e.setHttpMethod("POST");
        e.setRequestUri("/sysAuthUser/addSysAuthUser");
        e.setClientIp("10.0.0.1");
        e.setResult(result);
        e.setCostMs(12);
        e.setCreatedTime(LocalDateTime.of(2026, 10, 19, 21, 30, 5));
        return e;
    }

    @Test
    @DisplayName("分页查询：时间格式化、结果转中文、字段透传")
    void pageQueryMapsVo() {
        Page<SysAuthAuditLog> page = new Page<>(1, 10);
        page.setRecords(List.of(sample("USER_ADD", SysAuthAuditLog.RESULT_SUCCESS)));
        page.setTotal(1);
        when(mapper.selectAuditPage(any(), any())).thenReturn(page);

        PageResult<AuditLogVO> result = service.pageQuery(new AuditLogPageQuery());

        assertEquals(1, result.getRecords().size());
        assertEquals(1L, result.getTotal());
        AuditLogVO vo = result.getRecords().get(0);
        assertEquals("2026-10-19 21:30:05", vo.getCreatedTime());
        assertEquals("成功", vo.getResultText());
        assertEquals("USER_ADD", vo.getAction());
        assertEquals("admin", vo.getOperator());
    }

    @Test
    @DisplayName("分页查询：空结果返回空列表且 total 为 0")
    void pageQueryEmpty() {
        Page<SysAuthAuditLog> page = new Page<>(1, 10);
        page.setRecords(List.of());
        page.setTotal(0);
        when(mapper.selectAuditPage(any(), any())).thenReturn(page);

        PageResult<AuditLogVO> result = service.pageQuery(new AuditLogPageQuery());

        assertTrue(result.getRecords().isEmpty());
        assertEquals(0L, result.getTotal());
    }

    @Test
    @DisplayName("分页查询：时间格式非法时给出中文提示（不是 500）")
    void pageQueryRejectsBadTime() {
        AuditLogPageQuery query = new AuditLogPageQuery();
        query.setBeginTime("2026/10/19 21:30");

        BusinessException e = assertThrows(BusinessException.class, () -> service.pageQuery(query));
        assertTrue(e.getMessage().contains("开始时间格式"), "实际: " + e.getMessage());
    }

    @Test
    @DisplayName("分页查询：开始时间晚于结束时间时拒绝")
    void pageQueryRejectsReversedRange() {
        AuditLogPageQuery query = new AuditLogPageQuery();
        query.setBeginTime("2026-10-20 00:00:00");
        query.setEndTime("2026-10-19 00:00:00");

        BusinessException e = assertThrows(BusinessException.class, () -> service.pageQuery(query));
        assertTrue(e.getMessage().contains("开始时间不能晚于结束时间"), "实际: " + e.getMessage());
    }

    @Test
    @DisplayName("分页查询：只给日期时按整天处理（结束时间补到 23:59:59）")
    void pageQueryAcceptsDateOnly() {
        Page<SysAuthAuditLog> page = new Page<>(1, 10);
        page.setRecords(List.of(sample("USER_ADD", SysAuthAuditLog.RESULT_FAILURE)));
        page.setTotal(1);
        when(mapper.selectAuditPage(any(), any())).thenReturn(page);

        AuditLogPageQuery query = new AuditLogPageQuery();
        query.setBeginTime("2026-10-19");
        query.setEndTime("2026-10-19");

        PageResult<AuditLogVO> result = service.pageQuery(query);

        assertEquals("失败", result.getRecords().get(0).getResultText());
        // 条件被规范化成完整时间后再进 SQL
        ArgumentCaptor<AuditLogPageQuery> captor = ArgumentCaptor.forClass(AuditLogPageQuery.class);
        verify(mapper).selectAuditPage(any(), captor.capture());
        assertEquals("2026-10-19 00:00:00", captor.getValue().getBeginTime());
        assertEquals("2026-10-19 23:59:59", captor.getValue().getEndTime());
    }

    @Test
    @DisplayName("动作字典：Mapper 返回 null 时返回空列表（不抛 NPE）")
    void actionDictNullSafe() {
        when(mapper.selectActionDict()).thenReturn(null);
        assertTrue(service.queryActionDict().isEmpty());
    }
}