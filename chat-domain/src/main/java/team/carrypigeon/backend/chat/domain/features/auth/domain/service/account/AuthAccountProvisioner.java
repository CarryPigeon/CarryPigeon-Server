package team.carrypigeon.backend.chat.domain.features.auth.domain.service.account;

import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthAccount;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.member.ChannelMemberProvisioningApi;
import team.carrypigeon.backend.chat.domain.features.user.domain.api.provisioning.UserAccountProvisioningApi;

/**
 * 鉴权账号开通协作对象。
 * 职责：在账号创建后补齐用户资料、默认频道成员关系和系统频道成员关系。
 * 边界：只服务 auth 注册与邮箱验证码建号链路，不负责 token 签发或登录校验。
 */
class AuthAccountProvisioner {

    private final UserAccountProvisioningApi userAccountProvisioningApi;
    private final ChannelMemberProvisioningApi channelAccountProvisioningApi;

    AuthAccountProvisioner(
            UserAccountProvisioningApi userAccountProvisioningApi,
            ChannelMemberProvisioningApi channelAccountProvisioningApi
    ) {
        this.userAccountProvisioningApi = userAccountProvisioningApi;
        this.channelAccountProvisioningApi = channelAccountProvisioningApi;
    }

    /**
     * 为新建账号补齐默认业务资源。
     *
     * @param account 已创建的鉴权账号
     */
    void provisionAccount(AuthAccount account) {
        userAccountProvisioningApi.initializeProfile(
                account.id(),
                account.createdAt(),
                account.updatedAt()
        );
        channelAccountProvisioningApi.initializeMemberships(
                account.id(),
                account.createdAt()
        );
    }
}
