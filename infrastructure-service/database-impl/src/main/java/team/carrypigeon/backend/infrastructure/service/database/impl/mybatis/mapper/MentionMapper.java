package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.MentionEntity;

/**
 * 提及 Mapper。
 * 职责：提供 chat_mention 表的 MyBatis-Plus 访问入口。
 * 边界：仅供 database-impl 内部服务使用；唯一保留的显式 SQL 是批量插入，
 * 因为 MyBatis-Plus BaseMapper 没有多行插入能力。
 */
@Mapper
public interface MentionMapper extends BaseMapper<MentionEntity> {

    /**
     * 单条 SQL 批量插入提及记录。
     *
     * @param mentions 待插入提及实体
     * @return 受影响行数
     */
    @Insert("""
            INSERT INTO chat_mention (
                mention_id,
                channel_id,
                message_id,
                from_account_id,
                target_type,
                target_account_id,
                created_at,
                is_read
            )
            VALUES
            <foreach collection="mentions" item="mention" separator=",">
              (
                #{mention.mentionId},
                #{mention.channelId},
                #{mention.messageId},
                #{mention.fromAccountId},
                #{mention.targetType},
                #{mention.targetAccountId},
                #{mention.createdAt},
                #{mention.read}
              )
            </foreach>
            """)
    int insertAll(@Param("mentions") List<MentionEntity> mentions);
}
