package com.admintem.web.job;

import com.admintem.web.config.AdmintemProperties;
import com.admintem.web.mapper.SysAuthAuditLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 审计日志保留期清理任务。
 *
 * <p>默认关闭（{@code admintem.audit.retention-enabled=false}），开启后按
 * {@code retention-days} 分批删除过期记录：单批 1000 行、最多 100 批，
 * 避免一次大删除造成长事务和锁表。审计表没有逻辑删除列，这是唯一允许的物理删除路径。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "admintem.audit", name = "retention-enabled", havingValue = "true")
public class AuditRetentionJob {

    private static final int BATCH_SIZE = 1000;
    private static final int MAX_BATCHES = 100;

    private final SysAuthAuditLogMapper auditLogMapper;
    private final AdmintemProperties properties;

    @Scheduled(cron = "${admintem.audit.retention-cron:0 30 3 * * ?}")
    public void cleanExpiredAuditLogs() {
        int days = properties.getAudit().getRetentionDays();
        if (days <= 0) {
            log.warn("审计保留天数配置非法（{}），跳过本次清理", days);
            return;
        }
        LocalDateTime before = LocalDateTime.now().minusDays(days);
        long total = 0;
        for (int i = 0; i < MAX_BATCHES; i++) {
            int deleted = auditLogMapper.deleteBefore(before, BATCH_SIZE);
            total += deleted;
            if (deleted < BATCH_SIZE) {
                break;
            }
        }
        if (total > 0) {
            log.info("审计日志清理完成: cutoff={}, deleted={}", before, total);
        }
    }
}
