package team.carrypigeon.backend.chat.domain.features.message.domain.service;

import java.util.Collection;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelPin;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.Mention;

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
     * @param senderSnapshot 发送者展示快照
     * @param recipientAccountIds 目标账户 ID 列表
     */
    void publish(ChannelMessage message, MessageSenderSnapshot senderSnapshot, Collection<Long> recipientAccountIds);

    /**
     * 向指定账户实时分发消息更新事件。
     *
     * @param message 已持久化的更新后业务消息
     * @param senderSnapshot 发送者展示快照
     * @param recipientAccountIds 目标账户 ID 列表
     */
    default void publishUpdate(ChannelMessage message, MessageSenderSnapshot senderSnapshot, Collection<Long> recipientAccountIds) {
        publish(message, senderSnapshot, recipientAccountIds);
    }

    default void publishPin(ChannelPin pin, Collection<Long> recipientAccountIds) {
    }

    default void publishUnpin(ChannelPin pin, long unpinnedByAccountId, long unpinnedAt, Collection<Long> recipientAccountIds) {
    }

    default void publishMentionCreated(Mention mention, Collection<Long> recipientAccountIds) {
    }
}
