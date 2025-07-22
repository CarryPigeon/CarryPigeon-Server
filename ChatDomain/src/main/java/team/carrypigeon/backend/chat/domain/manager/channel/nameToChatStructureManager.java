package team.carrypigeon.backend.chat.domain.manager.channel;

import org.springframework.stereotype.Component;
import team.carrypigeon.backend.api.chat.domain.structure.CPChatStructure;

import java.util.Map;

@Component
public class nameToChatStructureManager {
    private final Map<String, CPChatStructure> mapper;

    public nameToChatStructureManager(Map<String, CPChatStructure> mapper) {
        this.mapper = mapper;
    }

    public CPChatStructure getChannel(String typeName){
        return mapper.get(typeName);
    }
}
