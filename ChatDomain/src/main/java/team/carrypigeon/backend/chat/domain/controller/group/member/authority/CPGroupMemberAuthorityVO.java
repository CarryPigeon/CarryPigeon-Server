package team.carrypigeon.backend.chat.domain.controller.group.member.authority;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPGroupMemberAuthorityVO {
    private long gid;
    private long uid;
    private int authority;
}
