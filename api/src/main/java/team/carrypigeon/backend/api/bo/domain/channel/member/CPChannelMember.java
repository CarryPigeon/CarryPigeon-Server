package team.carrypigeon.backend.api.bo.domain.channel.member;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class CPChannelMember {
    // 成员表id
    private long id;
    // 用户id
    private long uid;
    // 通道id
    private long cid;
    // 群昵称
    private String name;
    // 权限
    private CPChannelMemberAuthorityEnum authority;
    // 加入时间
    private LocalDateTime joinTime;
}