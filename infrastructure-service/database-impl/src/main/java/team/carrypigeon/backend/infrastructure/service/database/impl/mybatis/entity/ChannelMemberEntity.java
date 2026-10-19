package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelMemberRecord;

/**
 * 频道成员持久化实体。
 * 职责：承接 chat_channel_member 表字段与 MyBatis-Plus 映射。
 * 边界：仅供 database-impl 内部使用，不暴露到 database-api。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("chat_channel_member")
public class ChannelMemberEntity {

    @TableId(value = "member_id", type = IdType.INPUT)
    private Long memberId;
    private Long channelId;
    private Long accountId;
    private String role;
    private Instant joinedAt;

    /**
     * 将持久化实体转换为数据库记录契约。
     *
     * @return 对应数据库记录契约
     */
    public ChannelMemberRecord toRecord() {
        return new ChannelMemberRecord(
                memberId,
                channelId,
                accountId,
                role,
                joinedAt
        );
    }

    /**
     * 将数据库记录契约转换为持久化实体。
     *
     * @param record 频道成员数据库记录契约
     * @return 对应持久化实体
     */
    public static ChannelMemberEntity from(ChannelMemberRecord record) {
        return new ChannelMemberEntity(
                record.memberId(),
                record.channelId(),
                record.accountId(),
                record.role(),
                record.joinedAt()
        );
    }
}
