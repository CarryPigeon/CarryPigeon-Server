package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;

/**
 * 频道封禁持久化实体。
 * 职责：承接 chat_channel_ban 表字段与 MyBatis 映射。
 * 边界：仅供 database-impl 内部使用，不暴露到 database-api。
 */
@TableName("chat_channel_ban")
public class ChannelBanEntity {

    private Long channelId;
    private Long bannedAccountId;
    private Long operatorAccountId;
    private String reason;
    private Instant expiresAt;
    private Instant createdAt;
    private Instant revokedAt;

    public Long getChannelId() {
        return channelId;
    }

    public void setChannelId(Long channelId) {
        this.channelId = channelId;
    }

    public Long getBannedAccountId() {
        return bannedAccountId;
    }

    public void setBannedAccountId(Long bannedAccountId) {
        this.bannedAccountId = bannedAccountId;
    }

    public Long getOperatorAccountId() {
        return operatorAccountId;
    }

    public void setOperatorAccountId(Long operatorAccountId) {
        this.operatorAccountId = operatorAccountId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }
}
