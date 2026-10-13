package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity;

import java.time.Instant;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 频道封禁持久化实体。
 * 职责：承接 chat_channel_ban 表字段与 MyBatis 映射。
 * 边界：仅供 database-impl 内部使用，不暴露到 database-api。
 */
@TableName("chat_channel_ban")
@Data
public class ChannelBanEntity {

    @TableId
    private Long banId;
    private Long channelId;
    private Long bannedAccountId;
    private Long operatorAccountId;
    private String reason;
    private Instant expiresAt;
    private Instant createdAt;
    private Instant revokedAt;
}
