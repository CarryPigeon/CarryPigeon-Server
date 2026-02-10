package team.carrypigeon.backend.dao.database.mapper.channel.ban;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import team.carrypigeon.backend.api.bo.domain.channel.ban.CPChannelBan;

import java.time.LocalDateTime;

/**
 * `channel_ban` 表持久化对象。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("channel_ban")
public class ChannelBanPO {

    /**
     * 禁言记录 ID。
     */
    @TableId
    private Long id;

    /**
     * 频道 ID。
     */
    private Long cid;

    /**
     * 被禁言用户 ID。
     */
    private Long uid;

    /**
     * 操作者用户 ID。
     */
    private Long aid;

    /**
     * 禁言时长（秒）。
     */
    private int duration;

    /**
     * 禁言截止时间（毫秒时间戳）。
     */
    private Long untilTime;

    /**
     * 禁言原因。
     */
    private String reason;

    /**
     * 创建时间。
     */
    private LocalDateTime createTime;

    /**
     * 将 PO 转换为 BO。
     *
     * @return 频道禁言领域对象。
     */
    public CPChannelBan toBo() {
        return new CPChannelBan()
                .setId(id)
                .setCid(cid)
                .setUid(uid)
                .setAid(aid)
                .setDuration(duration)
                .setUntilTime(untilTime == null ? 0L : untilTime)
                .setReason(reason)
                .setCreateTime(createTime);
    }

    /**
     * 从 BO 构建 PO。
     *
     * @param channelBan 频道禁言领域对象。
     * @return 频道禁言持久化对象。
     */
    public static ChannelBanPO fromBo(CPChannelBan channelBan) {
        return new ChannelBanPO()
                .setId(channelBan.getId())
                .setCid(channelBan.getCid())
                .setUid(channelBan.getUid())
                .setAid(channelBan.getAid())
                .setDuration(channelBan.getDuration())
                .setUntilTime(channelBan.getUntilTime())
                .setReason(channelBan.getReason())
                .setCreateTime(channelBan.getCreateTime());
    }
}
