package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelInviteEntity;

/**
 * 频道邀请 Mapper。
 * 职责：提供 chat_channel_invite 表的显式 SQL 访问入口。
 * 边界：仅供 database-impl 内部服务使用。
 */
@Mapper
public interface ChannelInviteMapper {

    /**
     * 插入新的频道邀请。
     *
     * @param entity 邀请实体
     * @return 受影响行数
     */
    @Insert("""
            INSERT INTO chat_channel_invite (channel_id, application_id, invitee_account_id, inviter_account_id, reason, status, created_at, responded_at)
            VALUES (#{channelId}, #{applicationId}, #{inviteeAccountId}, #{inviterAccountId}, #{reason}, #{status}, #{createdAt}, #{respondedAt})
            """)
    int insert(ChannelInviteEntity entity);

    /**
     * 查询频道邀请。
     *
     * @param channelId 频道 ID
     * @param inviteeAccountId 被邀请账户 ID
     * @return 邀请实体；未命中时返回空
     */
    @Select("""
            SELECT channel_id, application_id, invitee_account_id, inviter_account_id, reason, status, created_at, responded_at
            FROM chat_channel_invite
            WHERE channel_id = #{channelId} AND invitee_account_id = #{inviteeAccountId}
            """)
    ChannelInviteEntity findByChannelIdAndInviteeAccountId(
            @Param("channelId") long channelId,
            @Param("inviteeAccountId") long inviteeAccountId
    );

    @Select("""
            SELECT channel_id, application_id, invitee_account_id, inviter_account_id, reason, status, created_at, responded_at
            FROM chat_channel_invite
            WHERE channel_id = #{channelId} AND application_id = #{applicationId}
            LIMIT 1
            """)
    ChannelInviteEntity findByChannelIdAndApplicationId(
            @Param("channelId") long channelId,
            @Param("applicationId") long applicationId
    );

    @Select("""
            SELECT channel_id, application_id, invitee_account_id, inviter_account_id, reason, status, created_at, responded_at
            FROM chat_channel_invite
            WHERE channel_id = #{channelId}
            ORDER BY created_at DESC
            """)
    java.util.List<ChannelInviteEntity> findByChannelId(@Param("channelId") long channelId);

    /**
     * 更新邀请记录。
     *
     * @param entity 邀请实体
     * @return 受影响行数
     */
    @Update("""
            UPDATE chat_channel_invite
            SET application_id = #{applicationId},
                inviter_account_id = #{inviterAccountId},
                reason = #{reason},
                status = #{status},
                created_at = #{createdAt},
                responded_at = #{respondedAt}
            WHERE channel_id = #{channelId} AND invitee_account_id = #{inviteeAccountId}
            """)
    int update(ChannelInviteEntity entity);
}
