package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 消息创建内部请求。
 *
 * @param cid 频道 ID。
 * @param domain 消息领域。
 * @param domainVersion 消息领域版本。
 * @param replyToMid 回复目标消息 ID。
 * @param data 消息载荷。
 * @param idempotencyKey 幂等键。
 */
public record MessageCreateRequest(String cid,
                                   String domain,
                                   String domainVersion,
                                   String replyToMid,
                                   JsonNode data,
                                   String idempotencyKey) {
}
