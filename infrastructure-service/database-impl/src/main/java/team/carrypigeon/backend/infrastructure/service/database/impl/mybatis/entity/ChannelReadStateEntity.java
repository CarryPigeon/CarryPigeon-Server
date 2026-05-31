package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import lombok.Data;

/**
 * 频道已读状态持久化实体。
 */
@TableName("chat_channel_read_state")
@Data
public class ChannelReadStateEntity {

    @TableId(value = "channel_id")
    private Long channelId;
    private Long accountId;
    private Long lastReadMessageId;
    private Instant lastReadTime;
    private Instant createdAt;
    private Instant updatedAt;
}
