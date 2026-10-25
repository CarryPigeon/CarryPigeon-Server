package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.util.List;
import java.util.Optional;

import team.carrypigeon.backend.infrastructure.service.database.api.model.AuthRefreshSessionRecord;

/**
 * 刷新会话数据库服务抽象。
 * 职责：提供刷新会话表级通用元能力与撤销扩展。
 * 边界：不暴露 JDBC、SQL 或具体数据库框架细节。
 */
public interface AuthRefreshSessionDatabaseService {

    /**
     * 写入新的刷新会话记录。
     *
     * @param record 待持久化会话记录
     */
    void insert(AuthRefreshSessionRecord record);

    /**
     * 批量写入刷新会话记录。
     *
     * @param records 待持久化会话记录
     */
    void insertAll(List<AuthRefreshSessionRecord> records);

    /**
     * 按会话 ID 删除记录。
     *
     * @param sessionId 会话 ID
     */
    void delete(long sessionId);

    /**
     * 按会话 ID 批量删除记录。
     *
     * @param sessionIds 会话 ID 列表
     */
    void deleteAll(List<Long> sessionIds);

    /**
     * 更新刷新会话记录。
     *
     * @param record 待更新会话记录
     */
    void update(AuthRefreshSessionRecord record);

    /**
     * 查询刷新会话表全部记录。
     * 说明：仅供元能力完整性与离线维护使用。
     *
     * @return 会话记录列表
     */
    List<AuthRefreshSessionRecord> list();

    /**
     * 按会话 ID 查询刷新会话。
     *
     * @param sessionId 刷新会话 ID
     * @return 命中时返回会话记录，未命中时返回空
     */
    Optional<AuthRefreshSessionRecord> findById(long sessionId);

    /**
     * 按会话 ID 批量查询刷新会话。
     *
     * @param sessionIds 会话 ID 列表
     * @return 命中的会话记录列表
     */
    List<AuthRefreshSessionRecord> findByIds(List<Long> sessionIds);

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
