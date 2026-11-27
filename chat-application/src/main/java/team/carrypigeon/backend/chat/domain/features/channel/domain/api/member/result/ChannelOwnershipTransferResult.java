package team.carrypigeon.backend.chat.domain.features.channel.domain.api.member.result;


import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;

/**
 * 频道所有权转移结果投影。
 * 职责：向协议层暴露转移完成后原所有者与新所有者各自的角色。
 * 边界：只承载转移结果，不承载转移前置校验；角色以枚举名形式给出，由协议层决定展示形态。
 *
 * @param channelId 发生转移的频道 ID
 * @param previousOwnerAccountId 原所有者账户 ID
 * @param previousOwnerRole 原所有者转移后的角色名，大写形式
 * @param newOwnerAccountId 新所有者账户 ID
 * @param newOwnerRole 新所有者的角色名，大写形式
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
