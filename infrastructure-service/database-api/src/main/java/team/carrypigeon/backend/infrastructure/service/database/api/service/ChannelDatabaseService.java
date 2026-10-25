package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.util.List;
import java.util.Optional;

import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelDiscoverRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelRecord;

/**
 * 频道数据库服务抽象。
 * 职责：提供频道表级通用元能力与频道查询扩展。
 * 边界：不暴露 JDBC、SQL 或具体数据库框架细节。
 */
public interface ChannelDatabaseService {

    /**
     * 写入新的频道记录。
     *
     * @param record 待持久化频道记录
     */
    void insert(ChannelRecord record);

    /**
     * 批量写入频道记录。
     *
     * @param records 待持久化频道记录
     */
    void insertAll(List<ChannelRecord> records);

    /**
     * 按频道 ID 删除记录。
     *
     * @param channelId 频道 ID
     */
    void delete(long channelId);

    /**
     * 按频道 ID 批量删除记录。
     *
     * @param channelIds 频道 ID 列表
     */
    void deleteAll(List<Long> channelIds);

    /**
     * 更新频道记录。
     *
     * @param record 待更新频道记录
     */
    void update(ChannelRecord record);

    /**
     * 查询频道表全部记录。
     * 说明：仅供元能力完整性与离线维护使用。
     *
     * @return 频道记录列表
     */
    List<ChannelRecord> list();

    /**
     * 按频道 ID 查询频道。
     *
     * @param channelId 频道 ID
     * @return 命中时返回频道记录
     */
    Optional<ChannelRecord> findById(long channelId);

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
    List<ChannelDiscoverRecord> discoverChannels(String keyword, Long cursorChannelId, int limit);
}
