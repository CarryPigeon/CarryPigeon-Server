package team.carrypigeon.backend.chat.domain.service.group.member;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.api.bo.domain.group.CPGroupBO;
import team.carrypigeon.backend.api.bo.domain.group.member.CPGroupMemberBO;
import team.carrypigeon.backend.api.connection.vo.CPPacket;
import team.carrypigeon.backend.api.connection.vo.CPResponse;
import team.carrypigeon.backend.api.dao.group.CPGroupDAO;
import team.carrypigeon.backend.api.dao.group.member.CPGroupMemberDAO;
import team.carrypigeon.backend.chat.domain.manager.channel.CPChannelManager;
import team.carrypigeon.backend.chat.domain.structure.group.CPGroupChatStructure;
import team.carrypigeon.backend.common.json.JsonNodeUtil;

@Component
@Slf4j
public class CPGroupMemberService {

    private final CPGroupDAO cpGroupDAO;

    private final CPGroupMemberDAO cpGroupMemberDAO;

    private final CPChannelManager cpUserToChannelManager;

    private final ObjectMapper objectMapper;

    private final CPGroupChatStructure cpGroupChatStructure;

    public CPGroupMemberService(CPGroupDAO cpGroupDAO, CPGroupMemberDAO cpGroupMemberDAO, CPChannelManager cpUserToChannelManager, ObjectMapper objectMapper, CPGroupChatStructure cpGroupChatStructure) {
        this.cpGroupDAO = cpGroupDAO;
        this.cpGroupMemberDAO = cpGroupMemberDAO;
        this.cpUserToChannelManager = cpUserToChannelManager;
        this.objectMapper = objectMapper;
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
            for (Long admin : cpGroupMemberDAO.getAdmins(gid)) {
                CPPacket cpPacket = new CPPacket()
                        .setId(-1)
                        .setRoute("/core/group/member/apply")
                        .setData(JsonNodeUtil.createJsonNode("gid", gid + ""));
                cpUserToChannelManager.getChannels( admin)
                        .forEach(channel -> {
                            try {
                                channel.write(objectMapper.writeValueAsString(cpPacket),true);
                            } catch (JsonProcessingException e) {
                                log.error(e.getMessage(), e);
                            }
                        });
            }
            return CPResponse.SUCCESS_RESPONSE;
        }
        return CPResponse.ERROR_RESPONSE;
    }

    public CPResponse rejectApply(long gid, long uid,long adminId) {
        // 判断用户是否为管理
        CPGroupBO group = cpGroupDAO.getById(gid);
        CPGroupMemberBO member = cpGroupMemberDAO.getMember(adminId, gid);
        if (group==null||member==null||(group.getOwner()!=adminId&&member.getAuthority()!=2)){
            return CPResponse.ERROR_RESPONSE.copy().setTextData("no permission");
        }
        CPGroupMemberBO apply = cpGroupMemberDAO.getMember(uid, gid);
        if (apply == null || apply.getState() != 1){
            return CPResponse.ERROR_RESPONSE.copy().setTextData("no apply or is member or has been rejected");
        }
        if (cpGroupMemberDAO.updateState(gid, uid, 3)) {
            CPPacket cpPacket = new CPPacket()
                    .setId(-1)
                    .setRoute("/core/group/member/apply/reject")
                    .setData(JsonNodeUtil.createJsonNode("gid", gid + ""));
            // 发送通知 通知本人和管理
            Long[] admins = cpGroupMemberDAO.getAdmins(gid);
            for (Long admin : admins) {
                cpUserToChannelManager.getChannels(admin)
                        .forEach(channel -> {
                            try {
                                channel.write(objectMapper.writeValueAsString(cpPacket),true);
                            } catch (JsonProcessingException e) {
                                log.error(e.getMessage(), e);
                            }
                        });
            }
            cpUserToChannelManager.getChannels(uid)
                    .forEach(channel -> {
                        try {
                            channel.write(objectMapper.writeValueAsString(cpPacket),true);
                        } catch (JsonProcessingException e) {
                            log.error(e.getMessage(), e);
                        }
                    });
            return CPResponse.SUCCESS_RESPONSE.copy();
        }
        return CPResponse.ERROR_RESPONSE.copy().setTextData("unknown error");
    }

    public CPResponse acceptApply(long gid, long uid,long adminId) {
        // 判断用户是否为管理
        CPGroupBO group = cpGroupDAO.getById(gid);
        CPGroupMemberBO member = cpGroupMemberDAO.getMember(adminId, gid);
        if (group==null||member==null||(group.getOwner()!=adminId&&member.getAuthority()!=2)){
            return CPResponse.ERROR_RESPONSE.copy().setTextData("no permission");
        }
        CPGroupMemberBO apply = cpGroupMemberDAO.getMember(uid, gid);
        if (apply == null || apply.getState() != 1){
            return CPResponse.ERROR_RESPONSE.copy().setTextData("no apply or is member or has been rejected");
        }
        if (cpGroupMemberDAO.updateState(gid, uid, 2)) {
            CPPacket cpPacket = new CPPacket()
                    .setId(-1)
                    .setRoute("/core/group/update")
                    .setData(JsonNodeUtil.createJsonNode("gid", gid + ""));
            // 发送通知 通知所有人
            cpGroupChatStructure.noticeMember(gid,cpPacket);
            return CPResponse.SUCCESS_RESPONSE.copy();
        }
        return CPResponse.ERROR_RESPONSE.copy().setTextData("unknown error");
    }

    public CPResponse kickout(long gid, long uid,long adminId) {
        CPGroupBO group = cpGroupDAO.getById(gid);
        CPGroupMemberBO adminMember = cpGroupMemberDAO.getMember(adminId, gid);
        if (group==null||adminMember==null||(group.getOwner()!=adminId&&adminMember.getAuthority()!=2)){
            return CPResponse.ERROR_RESPONSE.copy().setTextData("no permission");
        }
        CPGroupMemberBO member = cpGroupMemberDAO.getMember(uid, gid);
        if (member == null || member.getState() != 2){
            return CPResponse.ERROR_RESPONSE.copy().setTextData("no apply or is no member");
        }
        // 踢出人不能是群主
        if (member.getUid()==group.getOwner()){
            return CPResponse.ERROR_RESPONSE.copy().setTextData("no permission");
        }
        // 管理员不能被管理员踢出
        if (member.getAuthority()==2&&group.getOwner()!=adminId){
            return CPResponse.ERROR_RESPONSE.copy().setTextData("no permission");
        }
        // 踢出
        boolean b = cpGroupMemberDAO.deleteMember(member);
        if (b) {
            //通知所有人
            CPPacket cpPacket = new CPPacket()
                    .setId(-1)
                    .setRoute("/core/group/member/kickout")
                    .setData(JsonNodeUtil.createJsonNode("gid", gid + ""));
            cpGroupChatStructure.noticeMember(gid,cpPacket);
            // 通知当事人
            cpUserToChannelManager.getChannels(uid)
                    .forEach(channel -> {
                        try {
                            channel.write(objectMapper.writeValueAsString(cpPacket),true);
                        }catch (JsonProcessingException e){
                            log.error(e.getMessage(),e);
                        }
                    });
        }
        return CPResponse.ERROR_RESPONSE.copy();
    }

    /**
     * 修改用户权限
     * */
    public CPResponse updateAuthority(long gid, long uid, int authority,long ownerId) {
        CPGroupBO group = cpGroupDAO.getById(gid);
        if (group.getOwner()!=ownerId){
            return CPResponse.ERROR_RESPONSE.copy().setTextData("no permission");
        }
        CPGroupMemberBO member = cpGroupMemberDAO.getMember(uid, gid);
        if (member==null||member.getState()!=2){
            return CPResponse.ERROR_RESPONSE.copy().setTextData("no member or not member");
        }
        if (cpGroupMemberDAO.updateAuthority(gid, uid, authority)) {
            CPPacket cpPacket = new CPPacket()
                    .setId(-1)
                    .setRoute("/core/group/member/update")
                    .setData(JsonNodeUtil.createJsonNode("gid", gid + ""));
            // 获取所有群成员
            cpGroupChatStructure.noticeMember(gid,cpPacket);
            return CPResponse.SUCCESS_RESPONSE.copy();
        }
        return CPResponse.ERROR_RESPONSE.copy().setTextData("unknown error");
    }
}
