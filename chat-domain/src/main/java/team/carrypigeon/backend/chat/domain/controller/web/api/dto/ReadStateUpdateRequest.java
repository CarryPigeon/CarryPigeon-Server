package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

/**
 * Internal request object used by LiteFlow for read state update.
 */
public record ReadStateUpdateRequest(String cid, String lastReadMid, Long lastReadTime) {
}
