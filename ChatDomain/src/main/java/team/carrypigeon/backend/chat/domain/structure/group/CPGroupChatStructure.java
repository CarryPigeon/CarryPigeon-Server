package team.carrypigeon.backend.chat.domain.structure.group;

import team.carrypigeon.backend.api.chat.domain.structure.CPChatStructure;
import team.carrypigeon.backend.api.chat.domain.structure.CPChatStructureTag;
import team.carrypigeon.backend.api.connection.vo.CPPacket;

@CPChatStructureTag("core:group")
public class CPGroupChatStructure implements CPChatStructure {
    @Override
    public boolean verifyMember(long channelId, long userId) {
        return false;
    }

    @Override
    public boolean noticeMember(long channelId, CPPacket packet) {
        return false;
    }
}
