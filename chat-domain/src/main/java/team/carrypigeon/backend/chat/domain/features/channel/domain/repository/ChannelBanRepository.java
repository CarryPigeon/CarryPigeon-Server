package team.carrypigeon.backend.chat.domain.features.channel.domain.repository;

import java.util.Optional;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelBan;

/**
 * 频道封禁仓储抽象。
 * 职责：定义频道封禁记录的领域语义读写入口。
 * 边界：不暴露数据库实现细节，不与活跃成员投影混用。
 */
public interface ChannelBanRepository {

    /**
     * 查询频道下目标账户的封禁记录。
     *
     * @param channelId 频道 ID
     * @param bannedAccountId 被封禁账户 ID
     * @return 命中时返回封禁记录
     */
    Optional<ChannelBan> findByChannelIdAndBannedAccountId(long channelId, long bannedAccountId);

    /**
     * 保存新的封禁记录。
     *
     * @param channelBan 封禁记录
     */
    void save(ChannelBan channelBan);

    /**
     * 更新已存在的封禁记录。
     *
     * @param channelBan 封禁记录
     */
    void update(ChannelBan channelBan);
}
