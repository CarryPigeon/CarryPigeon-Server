package team.carrypigeon.backend.chat.domain.manager.channel;

import org.springframework.stereotype.Component;
import team.carrypigeon.backend.api.chat.domain.structure.CPChatStructure;

import java.util.Map;

@Component
public class NameToChatStructureManager {
    private final Map<String, CPChatStructure> mapper;

    public NameToChatStructureManager(Map<String, CPChatStructure> mapper) {
        this.mapper = mapper;
    }

    public CPChatStructure getChannel(String typeName){
        return mapper.get(typeName);
    }
}
