package com.admintem.web.config;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 审计日志异步写入配置。
 *
 * <p><b>为什么异步：</b>审计是旁路能力，但每次管理动作都要多一次数据库 round-trip。
 * 在写接口里同步插入会让"改一条权限"凭空多出几毫秒到几十毫秒，且与业务事务抢连接。
 * 放到独立线程池后，业务响应不再等待审计落库。</p>
 *
 * <p><b>为什么用独立线程池（而不是默认的 applicationTaskExecutor）：</b></p>
 * <ul>
 *   <li>相互隔离：审计堆积时不能挤占其他异步任务；</li>
 *   <li>便于观测：线程名统一 {@code audit-*}，日志与线程 dump 一眼可辨；</li>
 *   <li>拒绝策略明确：队列满时由**调用线程自己执行**（CallerRunsPolicy），
 *       宁可让这次请求慢一点，也不能丢审计记录——审计的第一原则是不丢。</li>
 * </ul>
 *
 * <p><b>MDC 传递：</b>日志里的 {@code traceId} 来自请求线程的 MDC，异步线程默认拿不到。
 * 这里用 {@link TaskDecorator} 在提交时抓取、执行时还原，保证审计日志仍能与
 * 接口日志、SQL 日志用同一个 traceId 串起来。</p>
 */
@Slf4j
@Configuration
@EnableAsync
public class AuditAsyncConfig {

    /** 审计线程池 bean 名（@Async("auditExecutor") 引用） */
    public static final String AUDIT_EXECUTOR = "auditExecutor";

    /**
     * 审计专用线程池。
     *
     * <p>单线程 + 大队列：审计写入是纯 IO 且量不大，串行反而避免并发写库与乱序；
     * 队列 2000 足够吸收突发（例如批量授权）。</p>
     *
     * @return 线程池
     */
    @Bean(name = AUDIT_EXECUTOR)
    public ThreadPoolTaskExecutor auditExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(5000);
        executor.setThreadNamePrefix("audit-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setTaskDecorator(mdcTaskDecorator());
        // 停机时等待队列排空，避免"最后一批评审记录丢失"
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        log.info("审计线程池已初始化: core=2, max=4, queue=5000, 拒绝策略=CallerRuns（不丢审计）");
        return executor;
    }

    /** 把提交线程的 MDC（traceId 等）带到执行线程 */
    private TaskDecorator mdcTaskDecorator() {
        return runnable -> {
            Map<String, String> context = MDC.getCopyOfContextMap();
            return () -> {
                Map<String, String> previous = MDC.getCopyOfContextMap();
                if (context != null) {
                    MDC.setContextMap(context);
                }
                try {
                    runnable.run();
                } finally {
                    if (previous == null) {
                        MDC.clear();
                    } else {
                        MDC.setContextMap(previous);
                    }
                }
            };
        };
    }
}
