package team.carrypigeon.backend.infrastructure.service.database.impl.persistence.channel.member;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 频道成员 Mapper。
 * 职责：提供 chat_channel_member 表的 MyBatis-Plus 访问入口。
 * 边界：仅供 database-impl 内部服务使用；insertAll 是通用批量写入元能力。
 */
@Mapper
public interface ChannelMemberMapper extends BaseMapper<ChannelMemberEntity> {

    /** 只投影候选账号，频道主记录撤下后即使成员尚未清理也不返回。 */
    @Select("""
            <script>
            SELECT cm.account_id FROM chat_channel_member cm
            JOIN chat_channel c ON c.channel_id = cm.channel_id
            WHERE cm.channel_id = #{channelId} AND cm.account_id IN
            <foreach collection="accountIds" item="accountId" open="(" separator="," close=")">
                #{accountId}
            </foreach>
            ORDER BY cm.account_id
            </script>
            """)
    List<Long> filterMemberAccountIds(@Param("channelId") long channelId, @Param("accountIds") List<Long> accountIds);

    @Select("""
            SELECT cm.channel_id FROM chat_channel_member cm
            JOIN chat_channel c ON c.channel_id = cm.channel_id
            WHERE cm.account_id = #{accountId}
            """)
    List<Long> findChannelIdsByAccountId(@Param("accountId") long accountId);

    /**
     * 单条 SQL 批量插入频道成员。
     *
     * @param records 待插入实体
     * @return 受影响行数
     */
    @Insert("""
            <script>
            INSERT INTO chat_channel_member (
                member_id, channel_id, account_id, role, joined_at
            )
            VALUES
            <foreach collection="records" item="record" separator=",">
              (
                #{record.memberId},
                #{record.channelId},
                #{record.accountId},
                #{record.role},
                #{record.joinedAt}
              )
            </foreach>
            </script>
            """)
    int insertAll(@Param("records") List<ChannelMemberEntity> records);
}
