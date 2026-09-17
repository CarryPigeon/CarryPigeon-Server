package team.carrypigeon.backend.chat.domain.features.user.support.persistence;

import java.util.List;
import java.util.Optional;
import team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
import team.carrypigeon.backend.infrastructure.service.database.api.user.profile.UserProfileRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.user.profile.UserProfileDatabaseService;

/**
 * 基于 database-api 的用户资料仓储适配器。
 * 职责：在 user feature 内完成领域模型与 database-api 契约模型之间的转换。
 * 边界：不包含 SQL 与数据库驱动细节，具体持久化由 database-impl 提供。
 */
public class DatabaseBackedUserProfileRepository implements UserProfileRepository {

    private final UserProfileDatabaseService userProfileDatabaseService;

    public DatabaseBackedUserProfileRepository(UserProfileDatabaseService userProfileDatabaseService) {
        this.userProfileDatabaseService = userProfileDatabaseService;
    }

    /**
     * 按账户 ID 查询资料。
     */
    @Override
    public Optional<UserProfile> findByAccountId(long accountId) {
        return userProfileDatabaseService.findByAccountId(accountId)
                .map(this::toDomainModel);
    }

    /**
     * 按账户 ID 集合查询资料。
     * 边界：批量过滤下推到 database-api，避免领域服务读取全部用户资料。
     */
    @Override
    public List<UserProfile> findByAccountIds(List<Long> accountIds) {
        return userProfileDatabaseService.findByAccountIds(accountIds).stream()
                .map(this::toDomainModel)
                .toList();
    }

    /**
     * 持久化新的用户资料。
     */
    @Override
    public UserProfile save(UserProfile userProfile) {
        userProfileDatabaseService.insert(toWriteRecord(userProfile));
        return userProfile;
    }

    /**
     * 更新既有用户资料。
     */
    @Override
    public UserProfile update(UserProfile userProfile) {
        userProfileDatabaseService.update(toWriteRecord(userProfile));
        return userProfile;
    }

    private UserProfile toDomainModel(UserProfileRecord record) {
        return new UserProfile(
                record.accountId(),
                record.nickname(),
                record.avatarUrl(),
                record.bio(),
                record.sex(),
                record.birthday(),
                record.createdAt(),
                record.updatedAt()
        );
    }

    private UserProfileRecord toWriteRecord(UserProfile userProfile) {
        return new UserProfileRecord(
                userProfile.accountId(),
                userProfile.nickname(),
                userProfile.avatarUrl(),
                userProfile.bio(),
                userProfile.sex(),
                userProfile.birthday(),
                userProfile.createdAt(),
                userProfile.updatedAt()
        );
    }
}
