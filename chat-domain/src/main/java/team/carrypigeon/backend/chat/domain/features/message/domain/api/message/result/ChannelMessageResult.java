package team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result;

/**
 * 频道消息 canonical 结果投影。
 * 职责：向协议层和跨 feature 调用方暴露统一消息字段。
 * 边界：不包含发送者资料快照、编辑字段或 domain 专属顶层字段。
 */
public record ChannelMessageResult(
        long messageId
) { }
