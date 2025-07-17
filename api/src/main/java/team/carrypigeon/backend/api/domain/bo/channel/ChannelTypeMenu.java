package team.carrypigeon.backend.api.domain.bo.channel;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ChannelTypeMenu {
    CORE("core"),PLUGINS("plugins");
    private final String typeName;

    public static ChannelTypeMenu valueOfByName(String typeName){
        for (ChannelTypeMenu channelTypeMenu : ChannelTypeMenu.values()) {
            if (channelTypeMenu.getTypeName().equals(typeName)) {
                return channelTypeMenu;
            }
        }
        return null;
    }
}