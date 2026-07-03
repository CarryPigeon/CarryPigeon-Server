package team.carrypigeon.backend.chat.domain.features.channel.domain.port;

/**
 * channel 侧使用的消息边界。
 * 职责：以频道语义暴露消息存在性和归属信息，避免 channel 领域服务直接理解 message 仓储模型。
 * 边界：只服务 channel feature 内部用例，不承载消息写入、搜索和实时发布。
 */
public interface ChannelMessageBoundary {

    /**
     * 读取频道读状态需要校验的消息快照。
     *
     * @param messageId 消息 ID
     * @return 消息归属快照
     */
    ChannelMessageSnapshot requireMessage(long messageId);

    /**
     * 判断频道是否已经存在消息。
     *
     * @param channelId 频道 ID
     * @return 存在任意消息时返回 true
     */
    boolean hasMessages(long channelId);

    /**
     * channel 侧关心的最小消息投影。
     *
     * @param messageId 消息 ID
     * @param channelId 消息所属频道 ID
     */
    record ChannelMessageSnapshot(long messageId, long channelId) {
    }
}
