package com.admintem.web.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link AfterCommitExecutor} 行为测试。
 *
 * <p>这是"缓存失效时机"的守门测试：必须在事务提交后才执行，
 * 否则并发读会把旧数据回填进缓存（见类注释中的时序图）。</p>
 */
class AfterCommitExecutorTest {

    private final AfterCommitExecutor executor = new AfterCommitExecutor();

    @Test
    @DisplayName("无事务时立即执行（保持改造前语义）")
    void runsImmediatelyWithoutTransaction() {
        AtomicInteger counter = new AtomicInteger();
        executor.run(counter::incrementAndGet);
        assertEquals(1, counter.get(), "没有事务时应当立即执行");
    }

    @Test
    @DisplayName("有事务时不立即执行，提交后才执行")
    void defersUntilCommit() {
        AtomicInteger counter = new AtomicInteger();
        TransactionSynchronizationManager.initSynchronization();
        try {
            executor.run(counter::incrementAndGet);
            assertEquals(0, counter.get(), "注册阶段绝不能执行（否则就是提交前失效缓存）");

            // 模拟回滚：afterCommit 不会被调用
            List<TransactionSynchronization> syncs =
                    TransactionSynchronizationManager.getSynchronizations();
            assertEquals(1, syncs.size());
            // afterCompletion(int) 带参数，必须用 lambda 而不是方法引用
            syncs.forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
            assertEquals(0, counter.get(), "回滚时不应清缓存");

            // 模拟提交
            syncs.forEach(TransactionSynchronization::afterCommit);
            assertEquals(1, counter.get(), "提交后必须执行");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("副作用抛异常不会冒泡（不能反过来影响已提交的业务）")
    void swallowsException() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            executor.run(() -> {
                throw new IllegalStateException("模拟 Redis 故障");
            });
            List<TransactionSynchronization> syncs =
                    TransactionSynchronizationManager.getSynchronizations();
            assertDoesNotThrow(() -> syncs.forEach(TransactionSynchronization::afterCommit));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("多次注册按注册顺序执行")
    void keepsRegistrationOrder() {
        List<String> order = new ArrayList<>();
        TransactionSynchronizationManager.initSynchronization();
        try {
            executor.run(() -> order.add("evict-user"));
            executor.run(() -> order.add("revoke-token"));
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);
            assertEquals(List.of("evict-user", "revoke-token"), order);
            assertTrue(true);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
