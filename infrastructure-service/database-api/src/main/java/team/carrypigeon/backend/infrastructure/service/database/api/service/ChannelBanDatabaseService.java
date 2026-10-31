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
     * 写入新的封禁记录。
     *
     * @param record 待持久化封禁记录
     */
    void insert(ChannelBanRecord record);

    /**
     * 批量写入封禁记录。
     *
     * @param records 待持久化封禁记录
     */
    void insertAll(List<ChannelBanRecord> records);

    /**
     * 按封禁 ID 删除记录。
     *
     * @param banId 封禁 ID
     */
    void delete(long banId);

    /**
     * 按封禁 ID 批量删除记录。
     *
     * @param banIds 封禁 ID 列表
     */
    void deleteAll(List<Long> banIds);

    /**
     * 更新既有封禁记录。
     *
     * @param record 待更新封禁记录
     */
    void update(ChannelBanRecord record);

    /**
     * 分页查询封禁记录。
     * 说明：按封禁 ID 升序返回，供离线维护与元能力完整性使用。
     *
     * @param page 分页参数
     * @return 分页封禁记录
     */
    PageResult<ChannelBanRecord> list(PageQuery page);

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
     * 查询频道下目标账户的封禁记录。
     *
     * @param channelId 频道 ID
     * @param bannedAccountId 被封禁账户 ID
     * @return 命中时返回封禁记录，未命中时返回空
     */
    Optional<ChannelBanRecord> findByChannelIdAndBannedAccountId(long channelId, long bannedAccountId);

    /**
     * 分页查询频道下的封禁记录。
     * 输出：按封禁 ID 倒序返回，供禁言列表分页使用。
     *
     * @param channelId 频道 ID
     * @param page 分页参数
     * @return 分页封禁记录
     */
    PageResult<ChannelBanRecord> pageBansByChannelId(long channelId, PageQuery page);

    /**
     * 删除频道下的全部封禁记录。
     *
     * @param channelId 频道 ID
     */
    void deleteByChannelId(long channelId);
}
