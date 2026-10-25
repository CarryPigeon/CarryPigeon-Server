package team.carrypigeon.backend.chat.domain.features.message.domain.api.message.input;

import java.util.List;
import java.util.Map;

/**
 * 通用频道消息发送命令。
 * 职责：表达已认证主体按 canonical envelope 发送任意受支持 domain 消息的输入。
 * 边界：所有 domain 专属字段必须位于 data，mentions 只承载提醒用户 ID；domain 已包含版本号。
 *
 * @param accountId 发送者账户 ID
 * @param channelId 目标频道 ID
 * @param domain 消息 domain，格式为主分类:子分类:版本号
 * @param data domain 专属 canonical 数据
 * @param mentions 需要提醒的用户 ID
 */
public record SendChannelMessageCommand(
        long accountId,
        long channelId,
        String domain,
        Map<String, Object> data,
        List<Long> mentions
) {
}
