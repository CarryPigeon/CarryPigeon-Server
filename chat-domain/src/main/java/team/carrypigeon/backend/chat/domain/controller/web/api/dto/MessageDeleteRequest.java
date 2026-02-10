package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

/**
 * 消息删除内部请求。
 *
 * @param mid 消息 ID。
 */
public record MessageDeleteRequest(String mid) {
}
