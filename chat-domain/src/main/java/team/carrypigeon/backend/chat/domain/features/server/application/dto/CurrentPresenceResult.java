package team.carrypigeon.backend.chat.domain.features.server.application.dto;

/**
 * 当前 presence 查询结果。
 * 职责：表达当前账户在本服务节点上的最小在线状态结果。
 * 边界：这里只反映本节点实时会话观测结果，不扩展到跨节点、最后在线时间或多设备明细。
 *
 * @param accountId 当前查询账户 ID
 * @param status presence 状态
 * @param onlineSessionCount 当前节点已观测到的在线会话数量
 */
public record CurrentPresenceResult(
        long accountId,
        CurrentPresenceStatus status,
        int onlineSessionCount
) {
}
