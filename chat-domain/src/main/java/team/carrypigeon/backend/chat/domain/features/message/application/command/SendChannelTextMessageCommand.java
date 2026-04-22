package team.carrypigeon.backend.chat.domain.features.message.application.command;

/**
 * 频道文本消息发送命令。
 * 职责：表达已认证主体发送文本消息的最小输入。
 * 边界：当前阶段只支持文本内容，不扩展文件或结构化消息输入。
 *
 * @param accountId 发送者账户 ID
 * @param channelId 目标频道 ID
 * @param body 文本正文
 */
public record SendChannelTextMessageCommand(long accountId, long channelId, String body) {
}
