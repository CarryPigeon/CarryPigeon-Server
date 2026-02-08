package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

/**
 * Internal request object used by LiteFlow for {@code PATCH /api/channels/{cid}}.
 */
public record ChannelPatchInternalRequest(String cid, ChannelPatchRequest body) {
}

