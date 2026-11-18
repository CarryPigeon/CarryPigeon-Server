package team.carrypigeon.backend.infrastructure.service.database.impl.persistence.channel;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelDiscoverRecord;
import team.carrypigeon.backend.infrastructure.service.database.impl.persistence.channel.ChannelEntity;

/**
 * 频道 Mapper。
 * 职责：提供 chat_channel 表的 MyBatis-Plus 访问入口。
 * 边界：仅供 database-impl 内部服务使用；insertAll 是通用批量写入元能力。
 */
@Mapper
public interface ChannelMapper extends BaseMapper<ChannelEntity> {

    /**
     * 普通频道发现页及其成员数；先限制频道窗口，再聚合本页成员，避免对全站成员先分组。
     * keyword 已转义 LIKE 通配符，按普通文本匹配名称和简介；频道 ID 游标为排他下界。
     * 头像暂无持久化来源，返回空；普通频道沿用申请审批加入规则。
     */
    @Select("""
            <script>
            SELECT c.channel_id AS id, c.name, c.brief, NULL AS avatar,
                   COUNT(cm.member_id) AS member_count, TRUE AS requires_application
            FROM (
                SELECT channel_id, name, brief FROM chat_channel
                WHERE is_system = FALSE
                <if test="keyword != null">
                  AND (name LIKE CONCAT('%', #{keyword}, '%') ESCAPE '!'
                       OR brief LIKE CONCAT('%', #{keyword}, '%') ESCAPE '!')
                </if>
                <if test="cursorChannelId != null">AND channel_id &gt; #{cursorChannelId}</if>
                ORDER BY channel_id ASC
                LIMIT #{limit}
            ) c
            LEFT JOIN chat_channel_member cm ON cm.channel_id = c.channel_id
            GROUP BY c.channel_id, c.name, c.brief
            ORDER BY c.channel_id ASC
            </script>
            """)
    List<ChannelDiscoverRecord> discoverChannels(@Param("keyword") String keyword,
            @Param("cursorChannelId") Long cursorChannelId, @Param("limit") int limit);

    /**
     * 单条 SQL 批量插入频道。
     *
     * @param records 待插入实体
     * @return 受影响行数
     */
    @Insert("""
            <script>
            INSERT INTO chat_channel (
                channel_id, name, brief, is_system, created_at, updated_at
            )
            VALUES
            <foreach collection="records" item="record" separator=",">
              (
                #{record.channelId},
                #{record.name},
                #{record.brief},
                #{record.systemChannel},
                #{record.createdAt},
                #{record.updatedAt}
              )
            </foreach>
            </script>
            """)
    int insertAll(@Param("records") List<ChannelEntity> records);
}
