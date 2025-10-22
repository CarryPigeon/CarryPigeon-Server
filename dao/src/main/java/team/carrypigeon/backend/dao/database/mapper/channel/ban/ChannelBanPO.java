package team.carrypigeon.backend.dao.database.mapper.channel.ban;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
    // 禁言状态，1为有效，2为失效
    private int state;
    // 禁言时长，单位为秒
    private int duration;
    // 创建时间
    private LocalDateTime createTime;
}