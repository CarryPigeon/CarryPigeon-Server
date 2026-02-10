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

/**
 * `channel_member` 表持久化对象。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("channel_member")
public class ChannelMemberPO {

    /**
     * 关系记录 ID。
     */
    @TableId
    private Long id;

    /**
     * 用户 ID。
     */
    private Long uid;

    /**
     * 频道 ID。
     */
    private Long cid;

    /**
     * 群昵称。
     */
    private String name;

    /**
     * 权限值。
     */
    private int authority;

    /**
     * 加入时间。
     */
    private LocalDateTime joinTime;

    /**
     * 从 BO 构建 PO。
     *
     * @param member 频道成员领域对象。
     * @return 频道成员持久化对象。
     */
    public static ChannelMemberPO from(CPChannelMember member) {
        return new ChannelMemberPO()
                .setId(member.getId())
                .setUid(member.getUid())
                .setCid(member.getCid())
                .setName(member.getName())
                .setAuthority(member.getAuthority().getAuthority())
                .setJoinTime(member.getJoinTime());
    }

    /**
     * 将 PO 转换为 BO。
     *
     * @return 频道成员领域对象。
     */
    public CPChannelMember toBo() {
        return new CPChannelMember()
                .setId(id)
                .setUid(uid)
                .setCid(cid)
                .setName(name)
                .setAuthority(CPChannelMemberAuthorityEnum.valueOf(authority))
                .setJoinTime(joinTime);
    }
}
