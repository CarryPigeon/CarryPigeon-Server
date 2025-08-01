package team.carrypigeon.backend.api.bo.domain.group.member;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 群聊成员BO
 * */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPGroupMemberBO {
    private long id;
    private long gid;
    private long uid;
    private int authority;
    private int state;
    private long time;
}
