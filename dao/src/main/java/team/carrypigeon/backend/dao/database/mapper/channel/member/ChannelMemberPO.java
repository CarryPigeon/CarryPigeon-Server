package team.carrypigeon.backend.dao.database.mapper.channel.member;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
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
    // 权限，0为普通成员，1为管理员
    private int authority;
    // 加入时间
    private LocalDateTime joinTime;

    public static ChannelMemberPO from(CPChannelMember member){
        return new ChannelMemberPO()
                .setId(member.getId())
                .setUid(member.getUid())
                .setCid(member.getCid())
                .setName(member.getName())
                .setAuthority(member.getAuthority().getAuthority())
                .setJoinTime(member.getJoinTime());
    }
    public CPChannelMember toBo(){
        return new CPChannelMember()
                .setId(id)
                .setUid(uid)
                .setCid(cid)
                .setName(name)
                .setAuthority(CPChannelMemberAuthorityEnum.valueOf(authority))
                .setJoinTime(joinTime);
    }
}
