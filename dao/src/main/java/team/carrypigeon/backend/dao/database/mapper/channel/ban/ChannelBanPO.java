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
 * {@code channel_ban} 表的持久化对象（PO）。
 * <p>
 * duration 单位：秒（seconds）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("channel_ban")
public class ChannelBanPO {
    // 禁言记录id
    @TableId
    private Long id;
    // 通道id
    private Long cid;
    // 用户id
    private Long uid;
    // 禁言的管理员id
    private Long aid;
    // 禁言时长，单位为秒
    private int duration;
    // 禁言截止时间（毫秒时间戳）
    private Long untilTime;
    // 禁言原因（可为空）
    private String reason;
    // 创建时间
    private LocalDateTime createTime;

    /**
     * 将当前 PO 转换为领域对象（BO）。
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
     * 从领域对象（BO）创建 PO。
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
