package team.carrypigeon.backend.chat.domain.features.channel.domain.command;

import java.time.Instant;

/**
 * 新账号基础频道成员关系初始化命令。
 *
 * @param accountId 新账号 ID
 * @param joinedAt 加入时间
 */
public record InitializeChannelMembershipsCommand(long accountId, Instant joinedAt) {
}
