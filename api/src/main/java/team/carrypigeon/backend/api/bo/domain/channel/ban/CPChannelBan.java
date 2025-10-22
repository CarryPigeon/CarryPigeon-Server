package team.carrypigeon.backend.api.bo.domain.channel.ban;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPChannelBan {
    // 禁言记录id
    private long id;
    // 通道id
    private long cid;
    // 用户id
    private long uid;
    // 禁言的管理员id
    private long aid;
    // 禁言状态
    private CPChannelBanStateEnum state;
    // 禁言时长，单位为秒
    private int duration;
    // 创建时间
    private LocalDateTime createTime;
}