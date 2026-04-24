package team.carrypigeon.backend.chat.domain.features.channel.application.command;

/**
 * 创建私有频道命令。
 * 职责：承载私有频道创建用例所需的最小输入。
 * 边界：只表达应用层命令，不承载创建编排逻辑。
 *
 * @param accountId 创建者账户 ID
 * @param name 频道名称
 */
public record CreatePrivateChannelCommand(long accountId, String name) {
}
