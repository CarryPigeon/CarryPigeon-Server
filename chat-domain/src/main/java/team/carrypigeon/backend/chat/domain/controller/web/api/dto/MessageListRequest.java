package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

/**
 * 消息列表查询内部请求。
 *
 * @param cid 频道 ID。
 * @param cursor 分页游标。
 * @param limit 单页条数。
 */
public record MessageListRequest(String cid, String cursor, Integer limit) {
}
