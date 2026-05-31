package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import lombok.Data;

/**
 * 频道置顶持久化实体。
 */
@TableName("chat_channel_pin")
@Data
public class ChannelPinEntity {
    private Long pinId;
    private Long channelId;
    private Long messageId;
    private Long pinnedByAccountId;
    private String note;
    private Instant pinnedAt;
}
