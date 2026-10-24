package team.carrypigeon.backend.infrastructure.basic.plugin.message;

/**
 * 消息生命周期状态。
 * 边界：消息不支持编辑，状态只允许从 sent 转换为 recalled。
 */
public enum CPMessageStatus {
    SENT,
    RECALLED
}
