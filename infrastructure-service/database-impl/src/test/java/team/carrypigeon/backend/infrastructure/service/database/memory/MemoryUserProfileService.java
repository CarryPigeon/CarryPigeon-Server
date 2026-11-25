package team.carrypigeon.backend.infrastructure.service.database.memory;

import static team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent.TEST;

import java.util.Optional;
import team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent;
import team.carrypigeon.backend.infrastructure.service.database.api.model.UserProfileRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.UserProfileDatabaseService;

/** UserProfile 的内存持久化实现，数据归当前测试容器所有。 */
@EnvironmentComponent(profiles = TEST)
public class MemoryUserProfileService extends LongIdTable<UserProfileRecord> implements UserProfileDatabaseService {

    public MemoryUserProfileService(MemoryDatabase database) {
        super(database, UserProfileRecord::accountId);
    }

    @Override
    public Optional<UserProfileRecord> findByIdForUpdate(long accountId) {
        return findById(accountId);
    }

    @Override
    public boolean containShareKeyReference(String shareKey) {
        return !select(r -> shareKey.equals(r.avatarUrl())).isEmpty();
    }
}
