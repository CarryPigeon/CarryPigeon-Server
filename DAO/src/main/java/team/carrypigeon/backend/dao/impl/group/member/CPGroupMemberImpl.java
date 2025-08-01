package team.carrypigeon.backend.dao.impl.group.member;

import org.springframework.stereotype.Component;
import team.carrypigeon.backend.api.bo.domain.group.CPGroupBO;
import team.carrypigeon.backend.api.bo.domain.group.member.CPGroupMemberBO;
import team.carrypigeon.backend.api.dao.group.member.CPGroupMemberDAO;

/**
 * TODO
 * */
@Component
public class CPGroupMemberImpl implements CPGroupMemberDAO {
    @Override
    public CPGroupMemberBO[] getMembers(long gid) {
        return new CPGroupMemberBO[0];
    }

    @Override
    public CPGroupBO[] getPrivateGroups(long uid) {
        return new CPGroupBO[0];
    }

    @Override
    public boolean updateMember(long gid, long uid, int authority) {
        return false;
    }

    @Override
    public boolean createApply(long gid, long uid) {
        return false;
    }

    @Override
    public boolean updateState(long gid, long uid, int state) {
        return false;
    }
}
