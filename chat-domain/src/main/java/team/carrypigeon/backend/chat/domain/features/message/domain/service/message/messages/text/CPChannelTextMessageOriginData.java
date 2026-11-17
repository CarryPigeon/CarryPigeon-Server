package team.carrypigeon.backend.chat.domain.features.message.domain.service.message.messages.text;

/**
 * 文本消息完成解析和校验后的不可变输入。
 * 边界：ID 在进入业务层前完成数值转换；null 表示不回复。
 *
 * @param content 保留原始空白的正文
 * @param replyToMid 可选的回复消息 ID
 */
public record CPChannelTextMessageOriginData(String content, Long replyToMid) {
}
