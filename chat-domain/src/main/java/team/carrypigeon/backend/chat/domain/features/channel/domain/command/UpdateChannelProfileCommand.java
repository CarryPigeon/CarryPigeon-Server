package team.carrypigeon.backend.chat.domain.features.channel.domain.command;

/**
 * 更新频道资料命令。
 * 职责：承载 `PATCH /api/channels/{cid}` 的最小输入。
 * 边界：只表达应用层命令，不承载权限规则。
 *
 * @param operatorAccountId 操作者账户 ID
 * @param channelId 频道 ID
 * @param name 频道名称
 * @param brief 频道简介
 */
public record UpdateChannelProfileCommand(long operatorAccountId, long channelId, String name, String brief) {
}
