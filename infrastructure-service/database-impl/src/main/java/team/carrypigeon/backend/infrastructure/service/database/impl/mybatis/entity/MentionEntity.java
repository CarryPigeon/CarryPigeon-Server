package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MentionRecord;

/**
 * 提及持久化实体。
 * 职责：承接 chat_mention 表字段与 MyBatis-Plus 映射。
 * 边界：仅供 database-impl 内部使用，不暴露到 database-api。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("chat_mention")
public class MentionEntity {

    @TableId(value = "mention_id", type = IdType.INPUT)
    private Long mentionId;
    private Long channelId;
    private Long messageId;
    private Long fromAccountId;
    private String targetType;
    private Long targetAccountId;
    private Instant createdAt;
    @TableField("is_read")
    private Boolean read;

    /**
     * 将持久化实体转换为数据库记录契约。
     *
     * @return 对应数据库记录契约
     */
    public MentionRecord toRecord() {
        return new MentionRecord(
                mentionId == null ? 0L : mentionId,
                channelId == null ? 0L : channelId,
                messageId == null ? 0L : messageId,
                fromAccountId == null ? 0L : fromAccountId,
                targetType,
                targetAccountId == null ? 0L : targetAccountId,
                createdAt,
                Boolean.TRUE.equals(read)
        );
    }

    /**
     * 将数据库记录契约转换为持久化实体。
     *
     * @param record 提及数据库记录契约
     * @return 对应持久化实体
     */
    public static MentionEntity from(MentionRecord record) {
        return new MentionEntity(
                record.mentionId(),
                record.channelId(),
                record.messageId(),
                record.fromAccountId(),
                record.targetType(),
                record.targetAccountId(),
                record.createdAt(),
                record.read()
        );
    }
}
