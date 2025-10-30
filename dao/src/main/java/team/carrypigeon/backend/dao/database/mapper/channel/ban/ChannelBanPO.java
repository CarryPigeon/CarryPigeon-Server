package team.carrypigeon.backend.dao.database.mapper.channel.ban;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.bo.domain.channel.ban.CPChannelBan;

import java.time.LocalDateTime;

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
    // 创建时间
    private LocalDateTime createTime;

    public CPChannelBan toBo() {
        return new CPChannelBan()
                .setId( id)
                .setCid(cid)
                .setUid(uid)
                .setAid(aid)
                .setDuration(duration)
                .setCreateTime(createTime);
    }

    public static ChannelBanPO fromBo(CPChannelBan channelBan) {
        return new ChannelBanPO()
                .setId(channelBan.getId())
                .setCid(channelBan.getCid())
                .setUid(channelBan.getUid())
                .setAid(channelBan.getAid())
                .setDuration(channelBan.getDuration())
                .setCreateTime(channelBan.getCreateTime());
    }
}