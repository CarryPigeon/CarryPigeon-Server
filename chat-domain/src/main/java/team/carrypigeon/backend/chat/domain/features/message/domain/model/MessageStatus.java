package team.carrypigeon.backend.chat.domain.features.message.domain.model;

/**
 * 消息生命周期状态。
 * 边界：消息不支持编辑，状态只允许从 sent 转换为 recalled。
 */
public enum MessageStatus {
    SENT,
    RECALLED
}
