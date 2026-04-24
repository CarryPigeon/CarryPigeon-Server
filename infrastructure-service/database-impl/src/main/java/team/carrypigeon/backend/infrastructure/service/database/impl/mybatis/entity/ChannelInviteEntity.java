package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;

/**
 * 频道邀请持久化实体。
 * 职责：承接 chat_channel_invite 表字段与 MyBatis 映射。
 * 边界：仅供 database-impl 内部使用，不暴露到 database-api。
 */
@TableName("chat_channel_invite")
public class ChannelInviteEntity {

    private Long channelId;
    private Long inviteeAccountId;
    private Long inviterAccountId;
    private String status;
    private Instant createdAt;
    private Instant respondedAt;

    public Long getChannelId() {
        return channelId;
    }

    public void setChannelId(Long channelId) {
        this.channelId = channelId;
    }

    public Long getInviteeAccountId() {
        return inviteeAccountId;
    }

    public void setInviteeAccountId(Long inviteeAccountId) {
        this.inviteeAccountId = inviteeAccountId;
    }

    public Long getInviterAccountId() {
        return inviterAccountId;
    }

    public void setInviterAccountId(Long inviterAccountId) {
        this.inviterAccountId = inviterAccountId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(Instant respondedAt) {
        this.respondedAt = respondedAt;
    }
}
