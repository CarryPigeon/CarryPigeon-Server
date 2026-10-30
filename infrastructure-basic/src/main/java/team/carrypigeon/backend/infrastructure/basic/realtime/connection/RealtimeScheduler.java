package team.carrypigeon.backend.infrastructure.basic.realtime.connection;

import java.time.Instant;

/**
 * 连接级定时器调度。
 * 职责：抽象认证超时与令牌过期的调度与取消，屏蔽底层定时器实现。
 */
public interface RealtimeScheduler {

    /**
     * 调度首次认证超时。
     * 语义：超时触发时若连接仍未认证，写出错误帧并关闭连接。
     *
     * @param timeoutSeconds 超时秒数
     */
    void scheduleAuthenticationTimeout(int timeoutSeconds);

    /**
     * 调度访问令牌过期。
     * 语义：到达过期时间时若连接仍活跃，写出错误帧并关闭连接。
     * 约束：重复调用时自动取消前一次调度。
     *
     * @param expiresAt 令牌过期时间
     */
    void scheduleAccessTokenExpiration(Instant expiresAt);

    /**
     * 取消认证超时。
     */
    void cancelAuthTimeout();
    /**
     * 取消访问令牌过期。
     */
    void cancelTokenExpiration();
}
