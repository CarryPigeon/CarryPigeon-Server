package team.carrypigeon.backend.chat.domain.features.message.domain.model;

/** 已持久化的发送请求身份；摘要用于识别同一请求 ID 的载荷冲突，消息 ID 用于返回原消息。 */
public record MessageSendRequest(String payloadHash, long messageId) {
}
