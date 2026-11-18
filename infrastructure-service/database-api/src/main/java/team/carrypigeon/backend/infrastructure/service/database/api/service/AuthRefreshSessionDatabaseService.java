package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.util.List;
import java.util.Optional;

import team.carrypigeon.backend.infrastructure.basic.page.PageQuery;
import team.carrypigeon.backend.infrastructure.basic.page.PageResult;
import team.carrypigeon.backend.infrastructure.service.database.api.model.AuthRefreshSessionRecord;

/**
 * 刷新会话数据库服务抽象。
 * 职责：提供刷新会话表级通用元能力与撤销扩展。
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
     * 按会话 ID 批量查询刷新会话。
     *
     * @param sessionIds 会话 ID 列表
     * @return 命中的会话记录列表
     */
    List<AuthRefreshSessionRecord> findByIds(List<Long> sessionIds);

    /**
     * 分页查询刷新会话记录。
     * 说明：按会话 ID 升序返回，供离线维护与元能力完整性使用。
     *
     * @param page 分页参数
     * @return 分页会话记录
     */
    PageResult<AuthRefreshSessionRecord> page(PageQuery page);

    /**
     * 写入新的刷新会话记录。
     *
     * @param record 待持久化会话记录
     * @return 恰好插入一条时为 true
     */
    boolean insert(AuthRefreshSessionRecord record);

    /**
     * 批量写入刷新会话记录。
     *
     * @param records 待持久化会话记录
     * @return 全部记录插入时为 true；空集合返回 true
     */
    boolean insertAll(List<AuthRefreshSessionRecord> records);

    /**
     * 更新刷新会话记录。
     *
     * @param record 待更新会话记录
     * @return 数据库报告恰好影响一条记录时为 true
     */
    boolean update(AuthRefreshSessionRecord record);

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

    /**
     * 按会话 ID 删除记录。
     *
     * @param sessionId 会话 ID
     * @return 恰好删除一条时为 true，未命中为 false
     */
    boolean deleteById(long sessionId);

    /**
     * 按会话 ID 批量删除记录。
     *
     * @param sessionIds 会话 ID 列表
     * @return 至少删除一条时为 true；空集合返回 true
     */
    boolean deleteByIds(List<Long> sessionIds);
}
