package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.util.Optional;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelInviteRecord;

/**
 * 频道邀请数据库服务抽象。
 * 职责：向 chat-domain 提供频道邀请记录的最小读写能力。
 * 边界：不暴露 JDBC、SQL 或具体数据库框架细节。
 */
public interface ChannelInviteDatabaseService {

    /**
     * 查询频道下目标账户的邀请记录。
     *
     * @param channelId 频道 ID
     * @param inviteeAccountId 被邀请账户 ID
     * @return 命中时返回邀请记录
     */
    Optional<ChannelInviteRecord> findByChannelIdAndInviteeAccountId(long channelId, long inviteeAccountId);

    /**
     * 写入新的邀请记录。
     *
     * @param record 待持久化邀请记录
     */
    void insert(ChannelInviteRecord record);

    /**
     * 更新已存在的邀请记录。
     *
     * @param record 待更新邀请记录
     */
    void update(ChannelInviteRecord record);
}
