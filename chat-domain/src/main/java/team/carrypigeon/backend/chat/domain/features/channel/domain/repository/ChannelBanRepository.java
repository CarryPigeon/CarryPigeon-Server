package team.carrypigeon.backend.chat.domain.features.channel.domain.repository;

import java.util.List;
import java.util.Optional;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelBan;

/**
 * 频道禁言仓储抽象。
 * 职责：定义频道禁言记录的领域语义读写入口。
 * 边界：不暴露数据库实现细节，不与活跃成员投影混用。
 */
public interface ChannelBanRepository {

    /**
     * 查询频道下目标账户的禁言记录。
     *
     * @param channelId 频道 ID
     * @param bannedAccountId 被禁言账户 ID
     * @return 命中时返回禁言记录
     */
    Optional<ChannelBan> findByRelation(long channelId, long bannedAccountId);

    /**
     * 查询频道下的全部禁言记录。
     */
    List<ChannelBan> list(long channelId);


    /**
     * 保存新的禁言记录。
     *
     * @param channelBan 禁言记录
     */
    void save(ChannelBan channelBan);

    /**
     * 更新已存在的禁言记录。
     *
     * @param channelBan 禁言记录
     */
    void update(ChannelBan channelBan);
}
