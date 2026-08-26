package team.carrypigeon.backend.chat.domain.features.message.domain.service;

import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelMessageResult;

/**
 * canonical 频道消息结果映射器。
 * 职责：统一领域消息到跨层只读投影的逐字段映射。
 * 边界：无状态且不读取仓储，不处理消息校验、持久化或事件发布。
 */
final class ChannelMessageProjectionMapper {

    private ChannelMessageProjectionMapper() {
    }

    static ChannelMessageResult toResult(ChannelMessage message) {
        return new ChannelMessageResult(
                message.messageId(),
                message.senderId(),
                message.channelId(),
                message.domain(),
                message.domainVersion(),
                message.data(),
                message.sendTime(),
                message.mentions(),
                message.preview(),
                message.status()
        );
    }
}
