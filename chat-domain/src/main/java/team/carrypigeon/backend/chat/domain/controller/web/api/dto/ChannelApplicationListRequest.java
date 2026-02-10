package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

/**
 * 入群申请列表查询内部请求。
 *
 * @param cid 频道 ID。
 */
public record ChannelApplicationListRequest(String cid) {
}
