package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

/**
 * Internal request object used by LiteFlow for channel ban endpoints.
 */
public record ChannelBanTargetRequest(String cid, String uid, ChannelBanUpsertRequest body) {
}

