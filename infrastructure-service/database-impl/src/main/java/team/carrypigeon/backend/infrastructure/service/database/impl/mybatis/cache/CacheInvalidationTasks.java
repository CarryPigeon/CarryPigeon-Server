package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import team.carrypigeon.backend.infrastructure.service.cache.api.service.CacheService;

/** 数据库缓存失效任务：与业务写入原子登记，独立事务领取并在失败后保留重试状态。 */
@Slf4j
public class CacheInvalidationTasks {
    private final JdbcClient jdbc;
    private final TransactionTemplate transaction;
    private final CacheService cache;

    public CacheInvalidationTasks(JdbcClient jdbc, PlatformTransactionManager transactions, CacheService cache) {
        this.jdbc = jdbc;
        this.cache = cache;
        this.transaction = new TransactionTemplate(transactions);
        // afterCommit 时原事务资源仍绑定在线程，必须开启新事务提交任务处理结果。
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** 只允许在业务写入事务内登记；回滚时任务随变更一起消失。 */
    public long schedule(String key) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("cache invalidation must be scheduled in a database transaction");
        }
        var generated = new GeneratedKeyHolder();
        jdbc.sql("INSERT INTO cache_invalidation_task(cache_key) VALUES (:key)")
                .param("key", key).update(generated, "task_id");
        return generated.getKey().longValue();
    }

    /** 提交后的处理失败只保留任务并记录日志，不把已经提交的业务响应改成失败。 */
    public void deliver(long taskId) {
        try {
            transaction.executeWithoutResult(status -> {
                var key = jdbc.sql("""
                        SELECT cache_key FROM cache_invalidation_task
                        WHERE task_id = :id AND next_attempt_at <= CURRENT_TIMESTAMP(3)
                        FOR UPDATE SKIP LOCKED
                        """).param("id", taskId).query(String.class).optional();
                if (key.isEmpty()) return;
                try {
                    if (cache != null) cache.delete(key.get());
                } catch (RuntimeException exception) {
                    jdbc.sql("""
                            UPDATE cache_invalidation_task SET attempts = attempts + 1,
                                next_attempt_at = TIMESTAMPADD(SECOND, 30, CURRENT_TIMESTAMP(3))
                            WHERE task_id = :id
                            """).param("id", taskId).update();
                    log.warn("缓存失效失败，30 秒后重试：taskId={}", taskId, exception);
                    return;
                }
                // Redis 删除幂等；删除后进程中断可再次领取，不能提前删除任务。
                jdbc.sql("DELETE FROM cache_invalidation_task WHERE task_id = :id").param("id", taskId).update();
            });
        } catch (RuntimeException exception) {
            log.error("缓存失效任务处理失败，保留持久化任务：taskId={}", taskId, exception);
        }
    }

    /** 按待处理状态读取，不使用自增 ID 高水位，因此不会跳过较晚提交的任务。 */
    @Scheduled(fixedDelay = 5000)
    public void retryPending() {
        jdbc.sql("""
                SELECT task_id FROM cache_invalidation_task
                WHERE next_attempt_at <= CURRENT_TIMESTAMP(3)
                ORDER BY next_attempt_at, task_id LIMIT 32
                """).query(Long.class).list().forEach(this::deliver);
    }
}
