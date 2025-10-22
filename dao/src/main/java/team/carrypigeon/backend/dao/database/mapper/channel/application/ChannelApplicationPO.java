package team.carrypigeon.backend.dao.database.mapper.channel.application;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("channel_application")
public class ChannelApplicationPO {
    // 申请表id
    @TableId
    private Long id;
    // 申请人id
    private Long uid;
    // 申请通道id
    private Long cid;
    // 处理人id
    private Long aid;
    // 申请状态，0为待处理，1为通过，2为拒绝
    private int state;
    // 申请信息留言
    private String msg;
    // 申请时间
    private LocalDateTime applyTime;
}
