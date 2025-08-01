package team.carrypigeon.backend.chat.domain.service.group;

import org.springframework.stereotype.Component;
import team.carrypigeon.backend.api.bo.domain.group.CPGroupBO;
import team.carrypigeon.backend.api.bo.domain.group.member.CPGroupMemberBO;
import team.carrypigeon.backend.api.dao.group.CPGroupDAO;
import team.carrypigeon.backend.api.dao.group.member.CPGroupMemberDAO;

@Component
public class CPGroupService {

    private final CPGroupDAO cpGroupDAO;

    private final CPGroupMemberDAO cpGroupMemberDAO;

    public CPGroupService(CPGroupDAO cpGroupDAO, CPGroupMemberDAO cpGroupMemberDAO) {
        this.cpGroupDAO = cpGroupDAO;
        this.cpGroupMemberDAO = cpGroupMemberDAO;
    }


    public CPGroupBO[] getUserGroups(long userId){
        CPGroupBO[] fixedGroups = cpGroupDAO.getFixedGroups();
        CPGroupBO[] privateGroups = cpGroupMemberDAO.getPrivateGroups(userId);
        CPGroupBO[] groups = new CPGroupBO[fixedGroups.length + privateGroups.length];
        System.arraycopy(fixedGroups, 0, groups, 0, fixedGroups.length);
        System.arraycopy(privateGroups, 0, groups, fixedGroups.length, privateGroups.length);
        return groups;
    }

    public CPGroupBO getGroupById(long gid){
        return cpGroupDAO.getById(gid);
    }

    public CPGroupMemberBO[] getMembersByGroupId(long gid){
        return cpGroupMemberDAO.getMembers(gid);
    }
}
