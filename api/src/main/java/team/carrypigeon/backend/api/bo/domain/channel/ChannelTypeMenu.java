package team.carrypigeon.backend.api.bo.domain.channel;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 聊天结构类型枚举
 * 共分核心类型与插件类型
 * 注:此类不应该被进一步拓展
 * */
@Getter
@AllArgsConstructor
public enum ChannelTypeMenu {
    CORE("core"),PLUGINS("plugins");
    private final String typeName;

    /**
     * 根据菜单名称获取对应的ChannelTypeMenu枚举值
     * 此方法用于在ChannelTypeMenu枚举中查找与给定名称匹配的枚举值
     * 如果找到匹配项，则返回对应的枚举值；如果没有找到匹配项，则返回null
     *
     * @param typeName 要查找的菜单名称
     * @return 对应的ChannelTypeMenu枚举值，如果找不到匹配项则返回null
     */
    public static ChannelTypeMenu valueOfByName(String typeName){
        for (ChannelTypeMenu channelTypeMenu : ChannelTypeMenu.values()) {
            if (channelTypeMenu.getTypeName().equals(typeName)) {
                return channelTypeMenu;
            }
        }
        return null;
    }
}