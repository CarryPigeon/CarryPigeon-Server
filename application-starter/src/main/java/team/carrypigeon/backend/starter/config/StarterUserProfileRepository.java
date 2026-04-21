package team.carrypigeon.backend.starter.config;

import java.util.Optional;
import team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
import team.carrypigeon.backend.infrastructure.service.database.api.model.UserProfileRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.UserProfileDatabaseService;

/**
 * 用户资料仓储运行时适配器。
 * 职责：在启动装配层完成 user 领域模型与 database-api 契约模型之间的转换。
 * 边界：不包含 SQL 与数据库驱动细节，具体持久化由 database-impl 提供。
 */
public class StarterUserProfileRepository implements UserProfileRepository {

    private final UserProfileDatabaseService userProfileDatabaseService;

    public StarterUserProfileRepository(UserProfileDatabaseService userProfileDatabaseService) {
        this.userProfileDatabaseService = userProfileDatabaseService;
    }

    @Override
    public Optional<UserProfile> findByAccountId(long accountId) {
        return userProfileDatabaseService.findByAccountId(accountId)
                .map(this::toDomainModel);
    }

    @Override
    public UserProfile save(UserProfile userProfile) {
        userProfileDatabaseService.insert(toRecord(userProfile));
        return userProfile;
    }

    @Override
    public UserProfile update(UserProfile userProfile) {
        userProfileDatabaseService.update(toRecord(userProfile));
        return userProfile;
    }

    private UserProfile toDomainModel(UserProfileRecord record) {
        return new UserProfile(
                record.accountId(),
                record.nickname(),
                record.avatarUrl(),
                record.bio(),
                record.createdAt(),
                record.updatedAt()
        );
    }

    private UserProfileRecord toRecord(UserProfile userProfile) {
        return new UserProfileRecord(
                userProfile.accountId(),
                userProfile.nickname(),
                userProfile.avatarUrl(),
                userProfile.bio(),
                userProfile.createdAt(),
                userProfile.updatedAt()
        );
    }
}
