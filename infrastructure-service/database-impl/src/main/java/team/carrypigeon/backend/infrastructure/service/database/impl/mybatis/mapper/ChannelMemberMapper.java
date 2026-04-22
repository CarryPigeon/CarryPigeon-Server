package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper;

import java.time.Instant;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
     * @param channelId 频道 ID
     * @param accountId 账户 ID
     * @param joinedAt 加入时间
     * @return 受影响行数
     */
    @Insert("""
            INSERT INTO chat_channel_member (channel_id, account_id, joined_at)
            VALUES (#{channelId}, #{accountId}, #{joinedAt})
            """)
    int insertMembership(
            @Param("channelId") long channelId,
            @Param("accountId") long accountId,
            @Param("joinedAt") Instant joinedAt
    );

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
