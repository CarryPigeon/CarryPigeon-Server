package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

/**
 * Internal request object used by LiteFlow for message listing.
 * <p>
 * {@code cursor} is an opaque string in HTTP layer (currently timestamp millis encoded as decimal string).
 */
public record MessageListRequest(String cid, String cursor, Integer limit) {
}
