package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelRecord;

/**
 * 频道持久化实体。
 * 职责：承接 chat_channel 表字段与 MyBatis-Plus 映射。
 * 边界：仅供 database-impl 内部使用，不暴露到 database-api。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("chat_channel")
public class ChannelEntity {

    @TableId
    private Long channelId;
    private String name;
    private String brief;
    private String avatar;
    @TableField("is_system")
    private Boolean systemChannel;
    private Instant createdAt;
    private Instant updatedAt;

    public ChannelRecord toRecord(){
        return new ChannelRecord(
            channelId,
            name,
            brief,
            avatar,
            systemChannel,
            createdAt,
            updatedAt
        );
    }

    public static ChannelEntity from(ChannelRecord record){
        return new ChannelEntity(
            record.id(),
            record.name(),
            record.brief(),
            record.avatar(),
            record.systemChannel(),
            record.createdAt(),
            record.updatedAt()
        );
    }
}
