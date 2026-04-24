package team.carrypigeon.backend.chat.domain.features.channel.domain.repository;

import java.util.Optional;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelInvite;

/**
 * 频道邀请仓储抽象。
 * 职责：定义频道邀请记录的领域语义读写入口。
 * 边界：不暴露数据库实现细节，也不把邀请压进活跃成员状态机。
 */
public interface ChannelInviteRepository {

    /**
     * 查询频道下目标账户的邀请记录。
     *
     * @param channelId 频道 ID
     * @param inviteeAccountId 被邀请账户 ID
     * @return 命中时返回邀请记录
     */
    Optional<ChannelInvite> findByChannelIdAndInviteeAccountId(long channelId, long inviteeAccountId);

    /**
     * 保存新的邀请记录。
     *
     * @param channelInvite 邀请记录
     */
    void save(ChannelInvite channelInvite);

    /**
     * 更新已存在的邀请记录。
     *
     * @param channelInvite 邀请记录
     */
    void update(ChannelInvite channelInvite);
}
