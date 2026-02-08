package team.carrypigeon.backend.dao.database.mapper.channel;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;

import java.time.LocalDateTime;

/**
 * {@code channel} 表的持久化对象（PO）。
 * <p>
 * 用于 BO（{@link CPChannel}）与数据库字段之间的转换。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("channel")
public class ChannelPO {
    // 通道id
    @TableId
    private Long id;
    //  通道名
    private String name;
    // 通道所有者
    private Long owner;
    // 通道简介
    private String brief;
    // 通道头像资源id
    private Long avatar;
    // 通道创建时间
    private LocalDateTime createTime;

    /**
     * 将当前 PO 转换为领域对象（BO）。
     */
    public CPChannel toBo() {
        return new CPChannel()
                .setId(id)
                .setName(name)
                .setOwner(owner)
                .setBrief(brief)
                .setAvatar(avatar)
                .setCreateTime(createTime);
    }

    /**
     * 从领域对象（BO）创建 PO。
     */
    public static ChannelPO fromBo(CPChannel cpChannel) {
        return new ChannelPO()
                .setId(cpChannel.getId())
                .setName(cpChannel.getName())
                .setOwner(cpChannel.getOwner())
                .setBrief(cpChannel.getBrief())
                .setAvatar(cpChannel.getAvatar())
                .setCreateTime(cpChannel.getCreateTime());
    }
}
