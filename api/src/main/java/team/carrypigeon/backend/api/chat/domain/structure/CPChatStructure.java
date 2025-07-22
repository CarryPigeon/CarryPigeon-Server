package team.carrypigeon.backend.api.chat.domain.structure;

import team.carrypigeon.backend.api.connection.vo.CPPacket;

/**
 * 聊天通道接口，私聊、群聊、订阅聊天均需实现一下接口
 * */
public interface CPChatStructure {
    /**
     * 校验是否为成员，即发送消息等的权力
     * */
    boolean verifyMember(long channelId, long userId);
    /**
     * 通知所有人相关消息
     * */
    boolean noticeMember(long channelId, CPPacket packet);
}