package team.carrypigeon.backend.chat.domain.features.server.application.dto;

/**
 * 当前 presence 状态枚举。
 * 职责：表达本服务节点对账户实时在线状态的最小稳定判断结果。
 */
public enum CurrentPresenceStatus {
    ONLINE,
    OFFLINE,
    UNAVAILABLE
}
