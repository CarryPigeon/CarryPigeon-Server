package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

/**
 * 频道禁言内部请求。
 *
 * @param cid 频道 ID。
 * @param uid 目标用户 ID。
 * @param body 禁言请求体。
 */
public record ChannelBanTargetRequest(String cid, String uid, ChannelBanUpsertRequest body) {
}
