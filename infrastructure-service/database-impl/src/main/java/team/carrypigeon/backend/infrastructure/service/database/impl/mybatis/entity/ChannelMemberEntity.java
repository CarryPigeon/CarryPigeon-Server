package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;

/**
 * 频道成员持久化实体。
 * 职责：承接 chat_channel_member 表字段与 MyBatis-Plus 映射。
 * 边界：仅供 database-impl 内部使用，不暴露到 database-api。
 */
@TableName("chat_channel_member")
public class ChannelMemberEntity {

    private Long channelId;
    private Long accountId;
    private String role;
    private Instant joinedAt;
    private Instant mutedUntil;

    public Long getChannelId() {
        return channelId;
    }

    public void setChannelId(Long channelId) {
        this.channelId = channelId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Instant getMutedUntil() {
        return mutedUntil;
    }

    public void setMutedUntil(Instant mutedUntil) {
        this.mutedUntil = mutedUntil;
    }
}
