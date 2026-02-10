package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

/**
 * 频道资料更新内部请求。
 *
 * @param cid 频道 ID。
 * @param body 频道更新请求体。
 */
public record ChannelPatchInternalRequest(String cid, ChannelPatchRequest body) {
}
