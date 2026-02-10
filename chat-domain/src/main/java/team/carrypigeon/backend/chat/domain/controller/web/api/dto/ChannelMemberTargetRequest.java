package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

/**
 * 频道成员目标内部请求。
 *
 * @param cid 频道 ID。
 * @param uid 目标用户 ID。
 */
public record ChannelMemberTargetRequest(String cid, String uid) {
}
