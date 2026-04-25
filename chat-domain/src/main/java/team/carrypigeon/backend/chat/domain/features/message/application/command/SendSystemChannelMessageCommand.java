package team.carrypigeon.backend.chat.domain.features.message.application.command;

/**
 * system 频道消息发送命令。
 * 职责：表达内部受信应用服务向 system 频道写入消息所需的最小输入。
 * 边界：这里只承载内部发送所需参数，不对外暴露为用户协议命令。
 *
 * @param operatorAccountId 触发 system 消息的账户 ID
 * @param channelId system 频道 ID
 * @param body 消息正文摘要
 * @param payload 结构化载荷 JSON
 * @param metadata 元数据 JSON
 */
public record SendSystemChannelMessageCommand(
        long operatorAccountId,
        long channelId,
        String body,
        String payload,
        String metadata
) {
}
