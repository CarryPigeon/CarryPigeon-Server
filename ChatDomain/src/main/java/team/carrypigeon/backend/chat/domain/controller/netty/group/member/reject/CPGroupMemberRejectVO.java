package team.carrypigeon.backend.chat.domain.controller.netty.group.member.reject;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPGroupMemberRejectVO {
    private long gid;
    private long uid;
}
