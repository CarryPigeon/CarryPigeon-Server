package team.carrypigeon.backend.chat.domain.features.message.domain.service;

import java.time.Instant;
import team.carrypigeon.backend.chat.domain.features.message.application.draft.ChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;

/**
 * 频道消息插件契约。
 * 职责：按消息类型把消息草稿转换为可持久化、可实时下发的领域消息。
 * 边界：插件只负责当前消息类型的校验与构造，不负责编排事务、频道校验或广播分发。
 */
public interface ChannelMessagePlugin {

    /**
     * 返回当前插件负责的消息类型。
     *
     * @return 稳定消息类型标识
     */
    String supportedType();

    /**
     * 根据上下文和草稿构造最终领域消息。
     *
     * @param context 消息构造上下文
     * @param draft 消息草稿
     * @return 最终领域消息
     */
    ChannelMessage createMessage(ChannelMessageBuildContext context, ChannelMessageDraft draft);

    /**
     * 消息构造上下文。
     * 职责：收敛插件构造最终消息时所需的稳定运行时信息。
     * 边界：这里只包含通用上下文，不携带协议层对象。
     *
     * @param messageId 消息 ID
     * @param serverId 服务端 ID
     * @param conversationId 会话 ID
     * @param channelId 频道 ID
     * @param senderId 发送者账户 ID
     * @param createdAt 创建时间
     */
    record ChannelMessageBuildContext(
            long messageId,
            String serverId,
            long conversationId,
            long channelId,
            long senderId,
            Instant createdAt
    ) {
    }
}
