package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

/**
 * 已读状态更新内部请求。
 *
 * @param cid 频道 ID。
 * @param lastReadMid 最后一条已读消息 ID。
 * @param lastReadTime 最后已读时间戳。
 */
public record ReadStateUpdateRequest(String cid, String lastReadMid, Long lastReadTime) {
}
