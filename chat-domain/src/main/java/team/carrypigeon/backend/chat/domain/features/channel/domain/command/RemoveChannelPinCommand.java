package team.carrypigeon.backend.chat.domain.features.channel.domain.command;

/**
 * 取消频道消息置顶命令。
 * 职责：承载 channel feature 定位置顶记录和校验治理权限所需输入。
 *
 * @param channelId 频道 ID
 * @param messageId 消息 ID
 * @param operatorAccountId 操作账号 ID
 */
public record RemoveChannelPinCommand(long channelId, long messageId, long operatorAccountId) {
}
