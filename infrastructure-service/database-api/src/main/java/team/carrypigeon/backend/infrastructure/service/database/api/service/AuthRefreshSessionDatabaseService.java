package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.util.Optional;

import team.carrypigeon.backend.infrastructure.service.database.api.model.AuthRefreshSessionRecord;

/**
 * 刷新会话数据库服务抽象。
 * 职责：向 chat-domain 提供刷新会话查询、写入与撤销能力。
 * 边界：不暴露 JDBC、SQL 或具体数据库框架细节。
 */
public interface AuthRefreshSessionDatabaseService {

    /**
     * 按会话 ID 查询刷新会话。
     *
     * @param sessionId 刷新会话 ID
     * @return 命中时返回会话记录，未命中时返回空
     */
    Optional<AuthRefreshSessionRecord> findById(long sessionId);

    /**
     * 写入新的刷新会话记录。
     *
     * @param record 待持久化会话记录
     */
    void insert(AuthRefreshSessionRecord record);

    /**
     * 撤销刷新会话。
     *
     * @param sessionId 刷新会话 ID
     */
    void revoke(long sessionId);

    /**
     * 仅在会话仍处于可用状态时原子撤销会话。
     *
     * @param sessionId 刷新会话 ID
     * @return 成功抢占撤销权时返回 true；已被其它请求撤销时返回 false
     */
    boolean revokeIfActive(long sessionId);
}
