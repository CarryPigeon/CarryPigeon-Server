package team.carrypigeon.backend.chat.domain.features.user.domain.api.provisioning;

import team.carrypigeon.backend.chat.domain.features.user.domain.api.provisioning.input.InitializeUserAccountProfileCommand;

import java.time.Instant;

/**
 * 用户账号资料初始化 API。
 * 职责：为新建账号创建所属 user feature 的初始公开资料。
 */
public interface UserAccountProvisioningApi {

    /**
     * 初始化新账号的用户资料。
     *
     * @param accountId 账号 ID
     * @param createdAt 创建时间
     * @param updatedAt 更新时间
     */
    void initializeProfile(long accountId, Instant createdAt, Instant updatedAt);
}
