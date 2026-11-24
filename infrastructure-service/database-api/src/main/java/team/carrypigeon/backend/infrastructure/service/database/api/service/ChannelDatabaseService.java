package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.util.List;
import java.util.Optional;

import team.carrypigeon.backend.infrastructure.service.database.api.model.DatabasePageQuery;
import team.carrypigeon.backend.infrastructure.service.database.api.model.DatabasePageResult;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelDiscoverRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelRecord;

/**
 * 频道数据库服务抽象。
 * 职责：提供频道表级通用元能力与频道查询扩展。
 * 边界：不暴露 JDBC、SQL 或具体数据库框架细节。
 */
public interface ChannelDatabaseService {

    /**
     * 按频道 ID 查询频道。
     *
     * @param channelId 频道 ID
     * @return 命中时返回频道记录
     */
    Optional<ChannelRecord> findById(long channelId);

    /** 授权专用直读；不读取或回填缓存，缺失时返回空。可见性遵守调用方事务隔离级别。 */
    Optional<ChannelRecord> findByIdDirect(long channelId);

    /**
     * 锁定频道并读取最新记录，不经过缓存；频道不存在时返回空。
     * 必须在事务内调用，锁持续到事务结束；治理写入先锁频道，再读取成员角色和修改关联数据。
     */
    Optional<ChannelRecord> findByIdForUpdate(long channelId);

    /**
     * 批量读取频道记录。
     *
     * @param channelIds 频道 ID 列表
     * @return 频道记录列表
     */
    List<ChannelRecord> findByIds(List<Long> channelIds);

    /**
     * 查询 system 频道。
     *
     * @return 命中时返回 system 频道记录
     */
    List<ChannelRecord> findSystemChannels();

    /**
     * 按发现页规则查询频道集合。
     *
     * @param keyword 关键字
     * @param cursorChannelId 游标频道 ID
     * @param limit 查询上限
     * @return 频道发现记录列表
     */
    List<ChannelDiscoverRecord> findPublicAfterId(String keyword, Long cursorChannelId, int limit);

    /**
     * 分页查询频道记录。
     * 说明：按频道 ID 升序返回，供离线维护与元能力完整性使用。
     *
     * @param page 分页参数
     * @return 分页频道记录
     */
    DatabasePageResult<ChannelRecord> page(DatabasePageQuery page);

    /**
     * 写入新的频道记录。
     *
     * @param record 待持久化频道记录
     * @return 恰好插入一条时为 true
     */
    boolean insert(ChannelRecord record);

    /**
     * 批量写入频道记录。
     *
     * @param records 待持久化频道记录
     * @return 全部记录插入时为 true；空集合返回 true
     */
    boolean insertAll(List<ChannelRecord> records);

    /**
     * 更新频道记录。
     *
     * @param record 待更新频道记录
     * @return 数据库报告恰好影响一条记录时为 true
     */
    boolean update(ChannelRecord record);

    /**
     * 按频道 ID 删除记录。
     *
     * @param channelId 频道 ID
     * @return 恰好删除一条时为 true，未命中为 false
     */
    boolean deleteById(long channelId);

    /**
     * 按频道 ID 批量删除记录。
     *
     * @param channelIds 频道 ID 列表
     * @return 至少删除一条时为 true；空集合返回 true
     */
    boolean deleteByIds(List<Long> channelIds);
}
