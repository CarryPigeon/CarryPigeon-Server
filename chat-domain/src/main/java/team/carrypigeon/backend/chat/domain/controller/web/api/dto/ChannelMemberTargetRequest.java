package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

/**
 * Internal request object used by LiteFlow for endpoints that target a specific member in a channel.
 */
public record ChannelMemberTargetRequest(String cid, String uid) {
}

