package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelMemberRecord;

import java.time.Instant;

/**
 * 频道成员持久化实体。
 * 职责：承接 chat_channel_member 表字段与 MyBatis-Plus 映射。
 * 边界：仅供 database-impl 内部使用，不暴露到 database-api。
 */
@Data
@TableName("chat_channel_member")
public class ChannelMemberEntity {
    @TableId
    private Long memberId;
    private Long channelId;
    private Long accountId;
    private String role;
    private Instant joinedAt;

    public ChannelMemberRecord toRecord() {
        return new ChannelMemberRecord(
            this.getMemberId(),
            this.getChannelId(),
            this.getAccountId(),
            this.getRole(),
            this.getJoinedAt()
        );
    }

    public static ChannelMemberEntity from(ChannelMemberRecord record) {
        ChannelMemberEntity entity = new ChannelMemberEntity();
        entity.setMemberId(record.memberId());
        entity.setChannelId(record.channelId());
        entity.setAccountId(record.accountId());
        entity.setRole(record.role());
        entity.setJoinedAt(record.joinedAt());
        return entity;
    }
}
