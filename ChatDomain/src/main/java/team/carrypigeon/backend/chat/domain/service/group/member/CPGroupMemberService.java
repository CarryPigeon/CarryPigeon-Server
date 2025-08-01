package team.carrypigeon.backend.chat.domain.service.group.member;

import org.springframework.stereotype.Component;
import team.carrypigeon.backend.api.bo.domain.group.CPGroupBO;
import team.carrypigeon.backend.api.bo.domain.group.member.CPGroupMemberBO;
import team.carrypigeon.backend.api.connection.vo.CPResponse;
import team.carrypigeon.backend.api.dao.group.CPGroupDAO;
import team.carrypigeon.backend.api.dao.group.member.CPGroupMemberDAO;
import team.carrypigeon.backend.chat.domain.structure.group.CPGroupChatStructure;

@Component
public class CPGroupMemberService {

    private final CPGroupDAO cpGroupDAO;

    private final CPGroupMemberDAO cpGroupMemberDAO;

    private final CPGroupChatStructure cpGroupChatStructure;

    public CPGroupMemberService(CPGroupDAO cpGroupDAO, CPGroupMemberDAO cpGroupMemberDAO, CPGroupChatStructure cpGroupChatStructure) {
        this.cpGroupDAO = cpGroupDAO;
        this.cpGroupMemberDAO = cpGroupMemberDAO;
        this.cpGroupChatStructure = cpGroupChatStructure;
    }

    public CPResponse createApply(long gid, long uid) {
        // 判断是否存在该成员状态
        CPGroupMemberBO member = cpGroupMemberDAO.getMember(uid, gid);
        if (member != null){
            // 判断是否已经被拒绝
            switch (member.getState()){
                case 1:
                    return CPResponse.ERROR_RESPONSE.copy().setTextData("already friend");
                case 2:
                    return CPResponse.ERROR_RESPONSE.copy().setTextData("already member");
                case 3:
                    member.setState(1);
                    cpGroupMemberDAO.updateState(gid,uid,1);
                    return CPResponse.SUCCESS_RESPONSE.copy();
                default:
                    return CPResponse.ERROR_RESPONSE.copy().setTextData("unknown error");
            }
        }
        // 判断group是否存在或者为固有群组
        CPGroupBO group = cpGroupDAO.getById(gid);
        if (group == null || group.getOwner() == -1) {
            return CPResponse.ERROR_RESPONSE.copy().setTextData("unexist group");
        }
        if (cpGroupMemberDAO.createApply(gid, uid)) {
            // TODO 通知所有管理
            return CPResponse.SUCCESS_RESPONSE;
        }
        return CPResponse.ERROR_RESPONSE;
    }
}
