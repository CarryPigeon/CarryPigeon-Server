package team.carrypigeon.backend.chat.domain.manager.channel;

import org.springframework.stereotype.Component;
import team.carrypigeon.backend.api.chat.domain.channel.CPChatChannel;

import java.util.Map;

@Component
public class CPChannelManager {
    private final Map<String, CPChatChannel> mapper;

    public CPChannelManager(Map<String, CPChatChannel> mapper) {
        this.mapper = mapper;
    }

    public CPChatChannel getChannel(String typeName){
        return mapper.get(typeName);
    }
}
