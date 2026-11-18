package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.util.List;
import java.util.Optional;

import team.carrypigeon.backend.infrastructure.basic.page.PageQuery;
import team.carrypigeon.backend.infrastructure.basic.page.PageResult;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelBanRecord;

/**
 * 频道封禁数据库服务抽象。
 * 职责：提供频道封禁表级通用元能力与封禁查询扩展。
 * 边界：不暴露 JDBC、SQL 或具体数据库框架细节。
 */
public interface ChannelBanDatabaseService {

    /**
     * 查询频道下目标账户的封禁记录。
     *
     * @param channelId 频道 ID
     * @param bannedAccountId 被封禁账户 ID
     * @return 命中时返回封禁记录，未命中时返回空
     */
    Optional<ChannelBanRecord> findByChannelIdAndBannedAccountId(long channelId, long bannedAccountId);

    /** 授权专用直读；不读取或回填缓存，缺失时返回空。可见性遵守调用方事务隔离级别。 */
    Optional<ChannelBanRecord> findByChannelIdAndBannedAccountIdDirect(long channelId, long bannedAccountId);

    /**
     * 按封禁 ID 查询记录。
     *
     * @param banId 封禁 ID
     * @return 命中时返回封禁记录，未命中时返回空
     */
    Optional<ChannelBanRecord> findById(long banId);

    /**
     * 按封禁 ID 批量查询记录。
     *
     * @param banIds 封禁 ID 列表
     * @return 命中的封禁记录列表
     */
    List<ChannelBanRecord> findByIds(List<Long> banIds);



    /**
     * 分页查询封禁记录。
     * 说明：按封禁 ID 升序返回，供离线维护与元能力完整性使用。
     *
     * @param page 分页参数
     * @return 分页封禁记录
     */
    PageResult<ChannelBanRecord> page(PageQuery page);

    /**
     * 分页查询频道下的封禁记录。
     * 输出：按封禁 ID 倒序返回，供禁言列表分页使用。
     *
     * @param channelId 频道 ID
     * @param page 分页参数
     * @return 分页封禁记录
     */
    PageResult<ChannelBanRecord> pageByChannelId(long channelId, PageQuery page);

    /**
     * 写入新的封禁记录。
     *
     * @param record 待持久化封禁记录
     * @return 恰好插入一条时为 true
     */
    boolean insert(ChannelBanRecord record);

    /**
     * 批量写入封禁记录。
     *
     * @param records 待持久化封禁记录
     * @return 全部记录插入时为 true；空集合返回 true
     */
    boolean insertAll(List<ChannelBanRecord> records);

    /**
     * 更新既有封禁记录。
     *
     * @param record 待更新封禁记录
     * @return 数据库报告恰好影响一条记录时为 true
     */
    boolean update(ChannelBanRecord record);

    /** 删除该频道最多 limit 条关系，返回已删除记录用于进度及通知；只用于已撤下频道的清理事务。 */
    List<ChannelBanRecord> deleteBatchByChannelId(long channelId, int limit);

    /**
     * 按封禁 ID 删除记录。
     *
     * @param banId 封禁 ID
     * @return 恰好删除一条时为 true，未命中为 false
     */
    boolean deleteById(long banId);

    /**
     * 按封禁 ID 批量删除记录。
     *
     * @param banIds 封禁 ID 列表
     * @return 至少删除一条时为 true；空集合返回 true
     */
    boolean deleteByIds(List<Long> banIds);

    /**
     * 删除频道下的全部封禁记录。
     *
     * @param channelId 频道 ID
     */
    void deleteByChannelId(long channelId);
}
