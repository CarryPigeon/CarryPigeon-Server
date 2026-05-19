package team.carrypigeon.backend.chat.domain.features.user.support.persistence;

import java.util.List;
import java.util.Optional;
import team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
import team.carrypigeon.backend.infrastructure.service.database.api.model.UserProfileRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.UserProfileDatabaseService;

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

    @Override
    public Optional<UserProfile> findByAccountId(long accountId) {
        return userProfileDatabaseService.findByAccountId(accountId)
                .map(this::toDomainModel);
    }

    @Override
    public List<UserProfile> findAll() {
        return userProfileDatabaseService.findAll().stream()
                .map(this::toDomainModel)
                .toList();
    }

    @Override
    public List<UserProfile> findByAccountIdBefore(Long cursorAccountId, int limit) {
        return userProfileDatabaseService.findByAccountIdBefore(cursorAccountId, limit).stream()
                .map(this::toDomainModel)
                .toList();
    }

    @Override
    public List<UserProfile> searchByKeyword(String keyword, Long cursorAccountId, int limit) {
        return userProfileDatabaseService.searchByKeyword(keyword, cursorAccountId, limit).stream()
                .map(this::toDomainModel)
                .toList();
    }

    @Override
    public UserProfile save(UserProfile userProfile) {
        userProfileDatabaseService.insert(toWriteRecord(userProfile));
        return userProfile;
    }

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
                userProfile.createdAt(),
                userProfile.updatedAt()
        );
    }
}
