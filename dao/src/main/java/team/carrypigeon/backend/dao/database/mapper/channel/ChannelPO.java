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
 * `channel` 表持久化对象。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("channel")
public class ChannelPO {

    /**
     * 频道 ID。
     */
    @TableId
    private Long id;

    /**
     * 频道名称。
     */
    private String name;

    /**
     * 频道所有者用户 ID。
     */
    private Long owner;

    /**
     * 频道简介。
     */
    private String brief;

    /**
     * 频道头像资源 ID。
     */
    private Long avatar;

    /**
     * 创建时间。
     */
    private LocalDateTime createTime;

    /**
     * 将 PO 转换为 BO。
     *
     * @return 频道领域对象。
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
     * 从 BO 构建 PO。
     *
     * @param cpChannel 频道领域对象。
     * @return 频道持久化对象。
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
