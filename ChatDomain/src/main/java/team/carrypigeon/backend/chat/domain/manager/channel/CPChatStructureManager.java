package team.carrypigeon.backend.chat.domain.manager.channel;

import org.springframework.stereotype.Component;
import team.carrypigeon.backend.api.chat.domain.structure.CPChatStructure;
import team.carrypigeon.backend.api.dao.channel.CPChatStructureTypeDAO;

import java.util.Map;

@Component
public class CPChatStructureManager {

    private final CPChatStructureTypeDAO cpChatStructureTypeDAO;

    private final Map<String, CPChatStructure> mapper;

    public CPChatStructureManager(CPChatStructureTypeDAO cpChatStructureTypeDAO, Map<String, CPChatStructure> mapper) {
        this.cpChatStructureTypeDAO = cpChatStructureTypeDAO;
        this.mapper = mapper;
    }

    public CPChatStructure getChannel(String typeName){
        System.out.println(typeName);
        return mapper.get(typeName);
    }

    public CPChatStructure getChannel(long channelId){
        return getChannel(cpChatStructureTypeDAO.getChatStructureType(channelId).toStringData());
    }
}