package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.util.Optional;
import team.carrypigeon.backend.infrastructure.service.database.api.model.AuthRefreshSessionRecord;

/**
 * 刷新会话数据库服务抽象。
 * 职责：向 chat-domain 提供刷新会话查询、保存与撤销能力。
 * 边界：不暴露 SQL、JDBC 或具体数据库实现。
 */
public interface AuthRefreshSessionDatabaseService {

    /**
     * 按会话 ID 查询刷新会话。
     *
     * @param sessionId 刷新会话 ID
     * @return 命中时返回会话记录
     */
    Optional<AuthRefreshSessionRecord> findById(long sessionId);

    /**
     * 写入刷新会话。
     *
     * @param record 刷新会话记录
     */
    void insert(AuthRefreshSessionRecord record);

    /**
     * 撤销刷新会话。
     *
     * @param sessionId 刷新会话 ID
     */
    void revoke(long sessionId);
}
