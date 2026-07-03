package team.carrypigeon.backend.chat.domain.features.message.domain.port;

/**
 * 消息发送者快照。
 * 职责：承载消息在持久化时刻对应的最小发送者展示信息，供 realtime 广播直接复用。
 * 边界：这里只保留消息事件所需的公开字段，不扩展为完整用户资料模型。
 *
 * @param accountId 发送者账户 ID
 * @param nickname 发送者昵称
 * @param avatarUrl 发送者头像
 */
public record MessageSenderSnapshot(long accountId, String nickname, String avatarUrl) {
}
