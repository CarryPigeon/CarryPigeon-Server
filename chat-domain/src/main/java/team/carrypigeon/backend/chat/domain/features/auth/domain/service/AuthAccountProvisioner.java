package team.carrypigeon.backend.chat.domain.features.auth.domain.service;

import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthAccount;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelAccountProvisioningApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.InitializeChannelMembershipsCommand;
import team.carrypigeon.backend.chat.domain.features.user.domain.api.UserAccountProvisioningApi;
import team.carrypigeon.backend.chat.domain.features.user.domain.command.InitializeUserAccountProfileCommand;

/**
 * 鉴权账号开通协作对象。
 * 职责：在账号创建后补齐用户资料、默认频道成员关系和系统频道成员关系。
 * 边界：只服务 auth 注册与邮箱验证码建号链路，不负责 token 签发或登录校验。
 */
class AuthAccountProvisioner {

    private final UserAccountProvisioningApi userAccountProvisioningApi;
    private final ChannelAccountProvisioningApi channelAccountProvisioningApi;

    AuthAccountProvisioner(
            UserAccountProvisioningApi userAccountProvisioningApi,
            ChannelAccountProvisioningApi channelAccountProvisioningApi
    ) {
        this.userAccountProvisioningApi = userAccountProvisioningApi;
        this.channelAccountProvisioningApi = channelAccountProvisioningApi;
    }

    /**
     * 为新建账号补齐默认业务资源。
     * 输入：已持久化的鉴权账号和用于初始化公开资料的昵称。
     * 副作用：创建用户资料、写入默认频道成员关系，并在缺失时写入 system 频道成员关系。
     * 失败语义：默认频道或 system 频道不存在时抛出领域问题异常，调用方应在事务内整体回滚。
     *
     * @param account 已创建的鉴权账号
     * @param nickname 初始用户资料昵称
     */
    void provisionAccount(AuthAccount account, String nickname) {
        userAccountProvisioningApi.initializeProfile(new InitializeUserAccountProfileCommand(
                account.id(),
                nickname,
                account.createdAt(),
                account.updatedAt()
        ));
        channelAccountProvisioningApi.initializeMemberships(new InitializeChannelMembershipsCommand(
                account.id(),
                account.createdAt()
        ));
    }
}
