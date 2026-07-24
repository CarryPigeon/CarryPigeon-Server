package team.carrypigeon.backend.chat.domain.features.message.domain.command;

import java.util.List;
import java.util.Map;

/**
 * system 频道消息发送命令。
 * 职责：表达内部受信领域服务向 system 频道写入消息所需的最小输入。
 * 边界：这里只承载内部发送所需参数，不对外暴露为用户协议命令。
 *
 * @param operatorAccountId 触发 system 消息的账户 ID
 * @param channelId system 频道 ID
 * @param domainVersion Core:System domain 版本
 * @param data Core:System 专属 canonical 数据
 * @param mentions 需要提醒的用户 ID
 */
public record SendSystemChannelMessageCommand(
        long operatorAccountId,
        long channelId,
        String domainVersion,
        Map<String, Object> data,
        List<Long> mentions
) {
}
