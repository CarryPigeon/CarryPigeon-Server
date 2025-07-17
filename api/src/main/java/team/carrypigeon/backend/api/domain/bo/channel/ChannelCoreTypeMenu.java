package team.carrypigeon.backend.api.domain.bo.channel;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 核心消息类型的枚举类，用于标识消息类型，用于消息的权限校验，现包括私聊、群聊等
 * */
@Getter
@AllArgsConstructor
public enum ChannelCoreTypeMenu {
    FRIEND("friend");
    private final String typeName;

    public static ChannelCoreTypeMenu valueOfByName(String typeName){
        for (ChannelCoreTypeMenu channelCoreTypeMenuEnum : ChannelCoreTypeMenu.values()) {
            if (channelCoreTypeMenuEnum.getTypeName().equals(typeName)) {
                return channelCoreTypeMenuEnum;
            }
        }
        return null;
    }
}
