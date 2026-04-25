package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import lombok.Data;

/**
 * 频道成员持久化实体。
 * 职责：承接 chat_channel_member 表字段与 MyBatis-Plus 映射。
 * 边界：仅供 database-impl 内部使用，不暴露到 database-api。
 */
@TableName("chat_channel_member")
@Data
public class ChannelMemberEntity {

    private Long channelId;
    private Long accountId;
    private String role;
    private Instant joinedAt;
    private Instant mutedUntil;
}
