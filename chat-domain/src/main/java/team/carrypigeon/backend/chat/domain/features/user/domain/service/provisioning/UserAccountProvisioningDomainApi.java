package team.carrypigeon.backend.chat.domain.features.user.domain.service.provisioning;

import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.user.domain.api.provisioning.UserAccountProvisioningApi;
import team.carrypigeon.backend.chat.domain.features.user.domain.api.provisioning.input.InitializeUserAccountProfileCommand;
import team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;

import java.time.Instant;

/**
 * 用户账号资料初始化 API 实现。
 * 职责：在 user feature 内创建新账号对应的初始公开资料。
 * 边界：不读取 auth 模型或频道仓储。
 */
@Service
public class UserAccountProvisioningDomainApi implements UserAccountProvisioningApi {

    private final UserProfileRepository userProfileRepository;

    public UserAccountProvisioningDomainApi(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    @Override
    public void initializeProfile(long accountId, Instant createdAt, Instant updatedAt) {
        userProfileRepository.save(UserProfile.initial(
                accountId,
                createdAt,
                updatedAt
        ));
    }
}
