package team.carrypigeon.backend.chat.domain.features.message.domain.command;

import java.util.List;
import java.util.Map;

/**
 * 通用频道消息发送命令。
 * 职责：表达已认证主体按 canonical envelope 发送任意受支持 domain 消息的输入。
 * 边界：所有 domain 专属字段必须位于 data，mentions 只承载提醒用户 ID。
 *
 * @param accountId 发送者账户 ID
 * @param channelId 目标频道 ID
 * @param domain 消息 domain
 * @param domainVersion domain 版本
 * @param data domain 专属 canonical 数据
 * @param mentions 需要提醒的用户 ID
 * @param clientMessageId 客户端消息 ID，可为空
 */
public record SendChannelMessageCommand(
        long accountId,
        long channelId,
        String domain,
        String domainVersion,
        Map<String, Object> data,
        List<Long> mentions,
        String clientMessageId
) {
}
