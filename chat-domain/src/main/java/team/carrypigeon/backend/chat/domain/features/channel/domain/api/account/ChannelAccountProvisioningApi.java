package team.carrypigeon.backend.chat.domain.features.channel.domain.api.account;



import team.carrypigeon.backend.chat.domain.features.channel.domain.api.account.input.InitializeChannelMembershipsCommand;

/**
 * 新账号频道成员关系初始化 API。
 * 职责：为新建账号建立默认频道与 system 频道成员关系。
 * 边界：不创建鉴权账号或用户资料。
 */
public interface ChannelAccountProvisioningApi {

    /**
     * 初始化新账号的基础频道成员关系。
     * 副作用：为账号保存默认频道与 system 频道成员关系。
     * TODO 待审核存在的必要性
     * @param command 成员关系初始化命令
     */
    void initializeMemberships(InitializeChannelMembershipsCommand command);
}
