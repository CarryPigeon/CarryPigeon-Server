package team.carrypigeon.backend.chat.domain.features.message.application.command;

/**
 * 撤回频道消息命令。
 * 职责：表达消息撤回用例所需的最小身份与资源参数。
 * 边界：不承载协议层与持久化细节。
 *
 * @param accountId 当前操作账户 ID
 * @param channelId 目标频道 ID
 * @param messageId 目标消息 ID
 */
public record RecallChannelMessageCommand(long accountId, long channelId, long messageId) {
}
