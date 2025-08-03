package team.carrypigeon.backend.chat.domain.controller.netty.group.member.kickout;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPGroupMemberKickoutVO {
    private long gid;
    private long uid;
}
