package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelBanEntity;

/**
 * 频道封禁 Mapper。
 * 职责：提供 chat_channel_ban 表的显式 SQL 访问入口。
 * 边界：仅供 database-impl 内部服务使用。
 */
@Mapper
public interface ChannelBanMapper {

    /**
     * 插入新的频道封禁。
     *
     * @param entity 封禁实体
     * @return 受影响行数
     */
    @Insert("""
            INSERT INTO chat_channel_ban (channel_id, banned_account_id, operator_account_id, reason, expires_at, created_at, revoked_at)
            VALUES (#{channelId}, #{bannedAccountId}, #{operatorAccountId}, #{reason}, #{expiresAt}, #{createdAt}, #{revokedAt})
            """)
    int insert(ChannelBanEntity entity);

    /**
     * 查询频道封禁。
     *
     * @param channelId 频道 ID
     * @param bannedAccountId 被封禁账户 ID
     * @return 封禁实体；未命中时返回空
     */
    @Select("""
            SELECT channel_id, banned_account_id, operator_account_id, reason, expires_at, created_at, revoked_at
            FROM chat_channel_ban
            WHERE channel_id = #{channelId} AND banned_account_id = #{bannedAccountId}
            """)
    ChannelBanEntity findByChannelIdAndBannedAccountId(
            @Param("channelId") long channelId,
            @Param("bannedAccountId") long bannedAccountId
    );

    /**
     * 更新封禁记录。
     *
     * @param entity 封禁实体
     * @return 受影响行数
     */
    @Update("""
            UPDATE chat_channel_ban
            SET operator_account_id = #{operatorAccountId},
                reason = #{reason},
                expires_at = #{expiresAt},
                created_at = #{createdAt},
                revoked_at = #{revokedAt}
            WHERE channel_id = #{channelId} AND banned_account_id = #{bannedAccountId}
            """)
    int update(ChannelBanEntity entity);
}
