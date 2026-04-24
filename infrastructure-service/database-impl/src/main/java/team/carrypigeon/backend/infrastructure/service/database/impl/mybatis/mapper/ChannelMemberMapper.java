package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelMemberEntity;

/**
 * 频道成员 Mapper。
 * 职责：提供 chat_channel_member 表的显式 SQL 访问入口。
 * 边界：仅供 database-impl 内部服务使用。
 */
@Mapper
public interface ChannelMemberMapper {

    /**
     * 插入新的频道成员关系。
     *
     * @param entity 成员实体
     * @return 受影响行数
     */
    @Insert("""
            INSERT INTO chat_channel_member (channel_id, account_id, role, joined_at, muted_until)
            VALUES (#{channelId}, #{accountId}, #{role}, #{joinedAt}, #{mutedUntil})
            """)
    int insertMembership(ChannelMemberEntity entity);

    /**
     * 查询成员关系是否存在。
     *
     * @param channelId 频道 ID
     * @param accountId 账户 ID
     * @return 命中数量
     */
    @Select("""
            SELECT COUNT(1)
            FROM chat_channel_member
            WHERE channel_id = #{channelId} AND account_id = #{accountId}
            """)
    long countMembership(@Param("channelId") long channelId, @Param("accountId") long accountId);

    /**
     * 查询活跃成员实体。
     *
     * @param channelId 频道 ID
     * @param accountId 账户 ID
     * @return 命中时返回成员实体
     */
    @Select("""
            SELECT channel_id, account_id, role, joined_at, muted_until
            FROM chat_channel_member
            WHERE channel_id = #{channelId} AND account_id = #{accountId}
            """)
    ChannelMemberEntity findByChannelIdAndAccountId(@Param("channelId") long channelId, @Param("accountId") long accountId);

    /**
     * 更新活跃成员实体。
     *
     * @param entity 成员实体
     * @return 受影响行数
     */
    @Update("""
            UPDATE chat_channel_member
            SET role = #{role},
                joined_at = #{joinedAt},
                muted_until = #{mutedUntil}
            WHERE channel_id = #{channelId} AND account_id = #{accountId}
            """)
    int updateMembership(ChannelMemberEntity entity);

    /**
     * 删除活跃成员实体。
     *
     * @param channelId 频道 ID
     * @param accountId 账户 ID
     * @return 受影响行数
     */
    @Delete("""
            DELETE FROM chat_channel_member
            WHERE channel_id = #{channelId} AND account_id = #{accountId}
            """)
    int deleteMembership(@Param("channelId") long channelId, @Param("accountId") long accountId);

    /**
     * 查询频道下的全部活跃成员实体。
     *
     * @param channelId 频道 ID
     * @return 成员实体列表
     */
    @Select("""
            SELECT channel_id, account_id, role, joined_at, muted_until
            FROM chat_channel_member
            WHERE channel_id = #{channelId}
            ORDER BY joined_at ASC, account_id ASC
            """)
    List<ChannelMemberEntity> findByChannelId(@Param("channelId") long channelId);

    /**
     * 查询频道下的全部成员账户 ID。
     *
     * @param channelId 频道 ID
     * @return 账户 ID 列表
     */
    @Select("""
            SELECT account_id
            FROM chat_channel_member
            WHERE channel_id = #{channelId}
            ORDER BY account_id ASC
            """)
    List<Long> findAccountIdsByChannelId(@Param("channelId") long channelId);
}
