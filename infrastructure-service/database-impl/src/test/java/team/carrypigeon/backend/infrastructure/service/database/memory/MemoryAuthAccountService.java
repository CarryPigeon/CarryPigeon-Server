package team.carrypigeon.backend.infrastructure.service.database.memory;

import static team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent.TEST;

import java.util.Optional;
import team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent;
import team.carrypigeon.backend.infrastructure.service.database.api.model.AuthAccountRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.AuthAccountDatabaseService;

/** AuthAccount 的内存持久化实现，数据归当前测试容器所有。 */
@EnvironmentComponent(profiles = TEST)
public class MemoryAuthAccountService extends LongIdTable<AuthAccountRecord> implements AuthAccountDatabaseService {

    public MemoryAuthAccountService(MemoryDatabase database) {
        super(database, AuthAccountRecord::id);
    }

    @Override
    boolean conflicts(AuthAccountRecord a, AuthAccountRecord b) {
        return a.email().equalsIgnoreCase(b.email()) || a.username().equalsIgnoreCase(b.username());
    }

    @Override
    public Optional<AuthAccountRecord> findByUsername(String username) {
        return select(r -> r.username().equalsIgnoreCase(username)).stream().findFirst();
    }

    @Override
    public Optional<AuthAccountRecord> findByEmail(String email) {
        return select(r -> r.email().equalsIgnoreCase(email)).stream().findFirst();
    }
}
