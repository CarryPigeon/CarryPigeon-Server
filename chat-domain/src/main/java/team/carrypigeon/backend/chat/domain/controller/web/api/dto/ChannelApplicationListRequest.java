package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

/**
 * Internal request object used by LiteFlow for {@code GET /api/channels/{cid}/applications}.
 */
public record ChannelApplicationListRequest(String cid) {
}

