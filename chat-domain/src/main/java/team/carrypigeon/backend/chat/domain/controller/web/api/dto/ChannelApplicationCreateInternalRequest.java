package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

/**
 * Internal request object used by LiteFlow for {@code POST /api/channels/{cid}/applications}.
 */
public record ChannelApplicationCreateInternalRequest(String cid, ChannelApplicationCreateRequest body) {
}

