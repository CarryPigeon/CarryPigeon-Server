package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

/**
 * Internal request object used by LiteFlow for {@code POST /api/channels/{cid}/applications/{application_id}/decisions}.
 */
public record ChannelApplicationDecisionInternalRequest(String cid, String applicationId, ChannelApplicationDecisionRequest body) {
}

