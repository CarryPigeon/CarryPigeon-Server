package team.carrypigeon.backend.infrastructure.service.database.api.model;

/** 消息发送幂等记录的持久化投影，不解释载荷摘要的业务含义。 */
public record MessageSendRequestRecord(String payloadHash, long messageId) {
}
