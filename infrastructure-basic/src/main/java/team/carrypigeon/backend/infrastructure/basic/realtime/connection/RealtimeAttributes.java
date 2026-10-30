package team.carrypigeon.backend.infrastructure.basic.realtime.connection;

/**
 * 实时服务会话上下文属性
 */
public interface RealtimeAttributes {
    /**
     * 设置会话ID
     * @param sessionId 会话id
     */
    void setSessionId(String sessionId);

    /**
     * 获取会话ID
     * @return 会话id
     */
    String getSessionId();

    /**
     * 设置用户ID
     * @param accountId 用户id
     */
    void setAccountId(Long accountId);

    /**
     * 获取用户ID
     * @return 用户id
     */
    Long getAccountId();
}
