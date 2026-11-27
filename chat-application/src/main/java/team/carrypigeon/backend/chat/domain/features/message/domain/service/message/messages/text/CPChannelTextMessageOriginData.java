package team.carrypigeon.backend.chat.domain.features.message.domain.service.message.messages.text;

/**
 * 文本消息的输入载荷。数字或字符串形式的回复 ID 在此统一以文本表示。
 *
 * @param content 保留原始空白的正文
 * @param replyToMid 回复消息 ID，null 表示不回复
 */
public record CPChannelTextMessageOriginData(String content, String replyToMid) {
}
