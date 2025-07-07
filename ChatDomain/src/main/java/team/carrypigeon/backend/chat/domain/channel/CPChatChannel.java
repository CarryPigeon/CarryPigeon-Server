package team.carrypigeon.backend.chat.domain.channel;

/**
 * 聊天通道接口，私聊、群聊、订阅聊天均需实现一下接口
 * */
public interface CPChatChannel {
    /**
     * @return 通道id，与数据库中的表结构一一对应
     * */
    long getChannelId();
}