package team.carrypigeon.backend.chat.domain.features.channel.domain.command;

import java.time.Instant;

/**
 * 设置频道消息置顶命令。
 * 职责：承载 channel feature 创建或替换置顶记录需要的稳定输入。
 * 边界：只携带消息标识，不引用 message feature 的模型或仓储。
 *
 * @param pinId 新置顶记录 ID
 * @param channelId 频道 ID
 * @param messageId 消息 ID
 * @param operatorAccountId 操作账号 ID
 * @param note 置顶备注
 * @param pinnedAt 置顶时间
 */
public record SetChannelPinCommand(
        long pinId,
        long channelId,
        long messageId,
        long operatorAccountId,
        String note,
        Instant pinnedAt
) {
}
