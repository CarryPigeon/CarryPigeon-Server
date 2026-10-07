package team.carrypigeon.backend.chat.domain.features.channel.domain.api.membership.result;


import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;

/**
 * 频道所有权转移结果。
 * 职责：向协议层暴露 OWNER 转移后的稳定角色结果。
 * 边界：只承载应用层返回数据，不承载治理逻辑。
 *
 * @param channelId 频道 ID
 * @param previousOwnerAccountId 原 OWNER 账户 ID
 * @param previousOwnerRole 原 OWNER 转移后的角色
 * @param newOwnerAccountId 新 OWNER 账户 ID
 * @param newOwnerRole 新 OWNER 的角色
 */
public record ChannelOwnershipTransferResult(
        long channelId,
        long previousOwnerAccountId,
        String previousOwnerRole,
        long newOwnerAccountId,
        String newOwnerRole
) {

    public static ChannelOwnershipTransferResult from(
            ChannelMember previousOwner,
            ChannelMember newOwner
    ) {
        return new ChannelOwnershipTransferResult(
                previousOwner.channelId(),
                previousOwner.accountId(),
                previousOwner.role().name(),
                newOwner.accountId(),
                newOwner.role().name()
        );
    }
}
