package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.util.List;
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
     * 按申请/邀请 ID 查询。
     */
    Optional<ChannelInviteRecord> findByChannelIdAndApplicationId(long channelId, long applicationId);

    /**
     * 查询频道下的全部申请/邀请记录。
     */
    List<ChannelInviteRecord> findByChannelId(long channelId);

    /** 判断频道是否存在任意邀请或申请记录。 */
    boolean existsByChannelId(long channelId);

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

    /**
     * 仅当当前状态仍为 PENDING 时更新申请，避免并发审批覆盖彼此结果。
     *
     * @return 实际更新一行时返回 {@code true}
     */
    default boolean updateIfPending(ChannelInviteRecord record) {
        update(record);
        return true;
    }
}
