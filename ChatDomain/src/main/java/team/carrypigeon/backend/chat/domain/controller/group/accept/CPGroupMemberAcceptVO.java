package team.carrypigeon.backend.chat.domain.controller.group.accept;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPGroupMemberAcceptVO {
    private long gid;
    private long uid;
}
