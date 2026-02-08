package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Internal request object used by LiteFlow for message creation.
 * <p>
 * This is built by controller from path/body and stored under {@link team.carrypigeon.backend.chat.domain.controller.web.api.flow.ApiFlowKeys#REQUEST}.
 */
public record MessageCreateRequest(String cid,
                                   String domain,
                                   String domainVersion,
                                   String replyToMid,
                                   JsonNode data,
                                   String idempotencyKey) {
}
