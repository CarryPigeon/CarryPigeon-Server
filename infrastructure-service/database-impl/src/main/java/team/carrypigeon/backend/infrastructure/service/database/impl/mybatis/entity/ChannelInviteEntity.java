package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import lombok.Data;

/**
 * 频道邀请持久化实体。
 * 职责：承接 chat_channel_invite 表字段与 MyBatis 映射。
 * 边界：仅供 database-impl 内部使用，不暴露到 database-api。
 */
@TableName("chat_channel_invite")
@Data
public class ChannelInviteEntity {

    private Long channelId;
    private Long inviteeAccountId;
    private Long inviterAccountId;
    private String status;
    private Instant createdAt;
    private Instant respondedAt;
}
