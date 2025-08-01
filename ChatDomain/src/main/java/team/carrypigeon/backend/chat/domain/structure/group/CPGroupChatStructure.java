package team.carrypigeon.backend.chat.domain.structure.group;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.group.CPGroupBO;
import team.carrypigeon.backend.api.bo.domain.group.member.CPGroupMemberBO;
import team.carrypigeon.backend.api.chat.domain.structure.CPChatStructure;
import team.carrypigeon.backend.api.chat.domain.structure.CPChatStructureTag;
import team.carrypigeon.backend.api.connection.vo.CPPacket;
import team.carrypigeon.backend.api.dao.group.CPGroupDAO;
import team.carrypigeon.backend.api.dao.group.member.CPGroupMemberDAO;
import team.carrypigeon.backend.chat.domain.manager.user.CPUserToChannelManager;

@Slf4j
@CPChatStructureTag("core:group")
public class CPGroupChatStructure implements CPChatStructure {

    private final CPGroupDAO cpGroupDAO;

    private final CPGroupMemberDAO cpGroupMemberDAO;

    private final CPUserToChannelManager cpUserToChannelManager;

    private final ObjectMapper objectMapper;

    public CPGroupChatStructure(CPGroupDAO cpGroupDAO, CPGroupMemberDAO cpGroupMemberDAO, CPUserToChannelManager cpUserToChannelManager, ObjectMapper objectMapper) {
        this.cpGroupDAO = cpGroupDAO;
        this.cpGroupMemberDAO = cpGroupMemberDAO;
        this.cpUserToChannelManager = cpUserToChannelManager;
        this.objectMapper = objectMapper;
    }


    @Override
    public boolean verifyMember(long channelId, long userId) {
        // 获取群组
        CPGroupBO byId = cpGroupDAO.getById(channelId);
        // 如果是创建者则直接返回true
        if (byId.getOwner() == userId) return true;
        // 获取群组成员
        CPGroupMemberBO[] members = cpGroupMemberDAO.getMembers(channelId);
        for (CPGroupMemberBO member : members) {
            if (member.getUid() == userId&&member.getState()==2) return true;
        }
        return false;
    }

    @Override
    public boolean noticeMember(long channelId, CPPacket packet) {
        for (CPGroupMemberBO member : cpGroupMemberDAO.getMembers(channelId)) {
            cpUserToChannelManager.getChannels(member.getUid()).forEach(
                    cpChannel -> {
                        try {
                            cpChannel.sendMessage(objectMapper.writeValueAsString(packet));
                        } catch (JsonProcessingException e) {
                            log.error("json write error:{}",packet,e);
                        }
                    }
            );
        }

        return false;
    }
}
