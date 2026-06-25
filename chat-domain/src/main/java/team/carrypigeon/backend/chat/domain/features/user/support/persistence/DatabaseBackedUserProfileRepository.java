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
     * 查询所有用户资料快照。
     * 边界：这里只做读取与模型转换，不承担权限裁剪。
     */
    @Override
    public List<UserProfile> findAll() {
        return userProfileDatabaseService.findAll().stream()
                .map(this::toDomainModel)
                .toList();
    }

    /**
     * 按账户游标倒序拉取资料列表。
     * 原因：供用户列表和分页接口使用稳定锚点。
     */
    @Override
    public List<UserProfile> findByAccountIdBefore(Long cursorAccountId, int limit) {
        return userProfileDatabaseService.findByAccountIdBefore(cursorAccountId, limit).stream()
                .map(this::toDomainModel)
                .toList();
    }

    /**
     * 按关键字搜索资料。
     * 输出：返回匹配关键字的领域资料集合。
     */
    @Override
    public List<UserProfile> searchByKeyword(String keyword, Long cursorAccountId, int limit) {
        return userProfileDatabaseService.searchByKeyword(keyword, cursorAccountId, limit).stream()
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
