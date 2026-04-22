package team.carrypigeon.backend.chat.domain.features.message.domain.service;

import java.util.Collection;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;

/**
 * 消息实时分发抽象。
 * 职责：定义频道消息持久化成功后的实时推送端口。
 * 边界：业务层只依赖抽象，不感知 Netty 会话实现细节。
 */
public interface MessageRealtimePublisher {

    /**
     * 向指定账户实时分发消息。
     *
     * @param message 已持久化的业务消息
     * @param recipientAccountIds 目标账户 ID 列表
     */
    void publish(ChannelMessage message, Collection<Long> recipientAccountIds);
}
