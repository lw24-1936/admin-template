package com.admintem.web.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * "事务提交后再执行"的小工具（缓存失效、令牌吊销等副作用统一走这里）。
 *
 * <p><b>为什么需要它（代码审查发现的一致性问题）：</b></p>
 * <p>此前"更新用户 → 清除权限缓存"是在事务方法体内直接调用的，也就是发生在
 * **提交之前**。并发下会出现这样的时序：</p>
 * <pre>
 *   线程A（更新）：删除缓存 …… 尚未提交
 *   线程B（读）  ：缓存未命中 → 回源数据库 → 读到【旧数据】→ 写回缓存（TTL 30 分钟）
 *   线程A（更新）：提交成功
 *   ⇒ 缓存里留着旧权限，最长 30 分钟内按钮权限都是错的
 * </pre>
 * <p>把副作用挂到 {@link TransactionSynchronization#afterCommit()} 之后，
 * 缓存只可能被"提交后的新数据"重建；回滚时则完全不清缓存（避免无谓抖动）。</p>
 *
 * <p><b>非事务场景：</b>当前线程没有活动事务时立即执行，保证行为与改造前一致
 * （也让既有单元测试无需改动）。</p>
 *
 * <p><b>异常处理：</b>副作用失败不能反过来影响已经提交的业务数据，因此这里捕获并记录；
 * 缓存最终会因 TTL 自然过期，属于可接受的最坏情况。</p>
 */
@Slf4j
@Component
public class AfterCommitExecutor {

    /**
     * 在事务提交后执行（无事务时立即执行）。
     *
     * @param action 需要在提交后执行的副作用（如清除缓存、吊销令牌）
     */
    public void run(Runnable action) {
        if (action == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // 没有事务：立即执行，保持既有语义
            executeQuietly(action);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                executeQuietly(action);
            }
        });
    }

    /** 执行并吞掉异常：副作用失败不应影响已提交的业务结果 */
    private void executeQuietly(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            log.warn("事务提交后的副作用执行失败（缓存可能短暂陈旧，将由 TTL 兜底）: {}", e.getMessage(), e);
        }
    }
}
