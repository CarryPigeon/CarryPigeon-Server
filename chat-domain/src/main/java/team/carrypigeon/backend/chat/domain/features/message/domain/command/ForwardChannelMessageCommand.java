package team.carrypigeon.backend.chat.domain.features.message.domain.command;

import java.util.List;

/**
 * 转发频道消息命令。
 *
 * @param accountId 转发账号 ID
 * @param sourceMessageId URL 锚点消息 ID
 * @param targetChannelId 目标频道 ID
 * @param comment 可选附言
 * @param mergedMessageIds 合并转发消息 ID；为空表示单条转发
 * @param idempotencyKey 幂等键，可为空
 */
public record ForwardChannelMessageCommand(
        long accountId,
        long sourceMessageId,
        long targetChannelId,
        String comment,
        List<Long> mergedMessageIds,
        String idempotencyKey
) {
}
