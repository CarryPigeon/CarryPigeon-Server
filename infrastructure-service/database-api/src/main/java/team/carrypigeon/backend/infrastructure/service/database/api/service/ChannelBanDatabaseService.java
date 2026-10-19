package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.util.List;
import java.util.Optional;

import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelBanRecord;

/**
 * 频道封禁数据库服务抽象。
 * 职责：向 chat-domain 提供频道封禁记录的最小读写能力。
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

    /**
     * 查询频道下的全部封禁记录。
     * 输出：按创建时间倒序返回，包含已解除与已过期记录。
     *
     * @param channelId 频道 ID
     * @return 封禁记录列表
     */
    List<ChannelBanRecord> findByChannelId(long channelId);

    /**
     * 写入新的封禁记录。
     *
     * @param record 待持久化封禁记录
     */
    void insert(ChannelBanRecord record);

    /**
     * 更新既有封禁记录。
     *
     * @param record 待更新封禁记录
     */
    void update(ChannelBanRecord record);
}
