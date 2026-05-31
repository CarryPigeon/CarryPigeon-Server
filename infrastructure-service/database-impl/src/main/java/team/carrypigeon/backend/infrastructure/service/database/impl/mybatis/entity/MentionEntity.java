package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import lombok.Data;

/**
 * 提及持久化实体。
 */
@TableName("chat_mention")
@Data
public class MentionEntity {
    @TableId("mention_id")
    private Long mentionId;
    private Long channelId;
    private Long messageId;
    private Long fromAccountId;
    private String targetType;
    private Long targetAccountId;
    private Instant createdAt;
    private Boolean read;
}
