package team.carrypigeon.backend.dao.database.mapper.channel.member;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("channel_member")
public class ChannelMemberPO {
    // 成员表id
    @TableId
    private Long id;
    // 用户id
    private Long uid;
    // 通道id
    private Long cid;
    // 群昵称
    private String name;
    // 权限，0为普通成员，1为管理员，2为被踢出
    private int authority;
    // 加入时间
    private LocalDateTime joinTime;
}
