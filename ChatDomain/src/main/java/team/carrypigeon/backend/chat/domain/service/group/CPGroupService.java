package team.carrypigeon.backend.chat.domain.service.group;

import org.springframework.stereotype.Component;
import team.carrypigeon.backend.api.bo.domain.group.CPGroupBO;
import team.carrypigeon.backend.api.bo.domain.group.member.CPGroupMemberBO;
import team.carrypigeon.backend.api.connection.vo.CPPacket;
import team.carrypigeon.backend.api.connection.vo.CPResponse;
import team.carrypigeon.backend.api.dao.group.CPGroupDAO;
import team.carrypigeon.backend.api.dao.group.member.CPGroupMemberDAO;
import team.carrypigeon.backend.chat.domain.structure.group.CPGroupChatStructure;
import team.carrypigeon.backend.common.id.IdUtil;
import team.carrypigeon.backend.common.json.JsonNodeUtil;

@Component
public class CPGroupService {

    private final CPGroupDAO cpGroupDAO;

    private final CPGroupMemberDAO cpGroupMemberDAO;

    private final CPGroupChatStructure cpGroupChatStructure;

    public CPGroupService(CPGroupDAO cpGroupDAO, CPGroupMemberDAO cpGroupMemberDAO, CPGroupChatStructure cpGroupChatStructure) {
        this.cpGroupDAO = cpGroupDAO;
        this.cpGroupMemberDAO = cpGroupMemberDAO;
        this.cpGroupChatStructure = cpGroupChatStructure;
    }


    /**
     * 获取指定用户能访问的所有群组数据
     * */
    public CPGroupBO[] getUserGroups(long userId){
        CPGroupBO[] fixedGroups = cpGroupDAO.getFixedGroups();
        CPGroupBO[] privateGroups = cpGroupMemberDAO.getPrivateGroups(userId);
        CPGroupBO[] groups = new CPGroupBO[fixedGroups.length + privateGroups.length];
        System.arraycopy(fixedGroups, 0, groups, 0, fixedGroups.length);
        System.arraycopy(privateGroups, 0, groups, fixedGroups.length, privateGroups.length);
        return groups;
    }

    /**
     * 通过id获取群组数据
     * */
    public CPGroupBO getGroupById(long gid){
        return cpGroupDAO.getById(gid);
    }

    /**
     * 通过群组id获取所有成员数据
     * */
    public CPGroupMemberBO[] getMembersByGroupId(long gid){
        return cpGroupMemberDAO.getMembers(gid);
    }

    /**
     * 通知所有人群组更新
     * */
    public void noticeAllGroupUpdate(long gid){
        CPPacket pack = new CPPacket()
                .setId(-1)
                .setRoute("/core/group/update")
                .setData(
                        JsonNodeUtil.createJsonNode("gid", gid + "")
                );
        cpGroupChatStructure.noticeMember(gid, pack);
    }

    /**
     * 通过用户id获取群组
     * */
    public long createGroup(long userId){
        long id = IdUtil.generateId();
        CPGroupBO cpGroupBO = new CPGroupBO()
                .setId(id)
                .setName("group-"+id)
                .setProfile(-1)
                .setOwner(userId)
                .setIntroduction("")
                .setStateId(IdUtil.generateId())
                .setRegisterTime(System.currentTimeMillis());
        return cpGroupDAO.createGroup(cpGroupBO)?id:-1;
    }

    /**
     * 移交群组
     * */
    public CPResponse giveGroup(long gid, long fromId , long toId){
        CPGroupBO group = getGroupById(gid);
        if (group==null||group.getOwner()!=fromId){
            return CPResponse.ERROR_RESPONSE.copy();
        }
        group.setOwner(toId);
        group.setStateId(IdUtil.generateId());
        if (!cpGroupDAO.updateGroup(group)){
            return CPResponse.ERROR_RESPONSE.copy();
        }
        // 通知所有人
        noticeAllGroupUpdate(gid);
        return CPResponse.SUCCESS_RESPONSE.copy();
    }
    /**
     * 删除群组
     * */
    public CPResponse deleteGroup(long gid, long userId){
        CPGroupBO group = getGroupById(gid);
        if (group==null||group.getOwner()!=userId){
            return CPResponse.ERROR_RESPONSE.copy();
        }
        if (!cpGroupDAO.deleteGroup(gid)){
            return CPResponse.ERROR_RESPONSE.copy();
        }
        noticeAllGroupUpdate(gid);
        return CPResponse.SUCCESS_RESPONSE.copy();
    }

    /**
     * 群组信息修改
     * */
    public CPResponse updateGroup(long gid, long userId, String name, String introduction, long profile){
        CPGroupBO group = getGroupById(gid);
        CPGroupMemberBO member = cpGroupMemberDAO.getMember(userId, gid);
        if (group==null||member==null||(group.getOwner()!=userId&&member.getAuthority()!=2)){
            return CPResponse.ERROR_RESPONSE.copy();
        }
        group.setName(name)
                .setIntroduction(introduction)
                .setProfile(profile)
                .setStateId(IdUtil.generateId());
        if (!cpGroupDAO.updateGroup(group)){
            return CPResponse.ERROR_RESPONSE.copy();
        }
        // 通知所有人
        noticeAllGroupUpdate(gid);
        return CPResponse.SUCCESS_RESPONSE.copy();
    }
}
