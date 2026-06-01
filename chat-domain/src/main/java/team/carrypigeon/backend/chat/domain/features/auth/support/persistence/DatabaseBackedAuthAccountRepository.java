package team.carrypigeon.backend.chat.domain.features.auth.support.persistence;

import java.util.Optional;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthAccount;
import team.carrypigeon.backend.chat.domain.features.auth.domain.repository.AuthAccountRepository;
import team.carrypigeon.backend.infrastructure.service.database.api.model.AuthAccountRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.AuthAccountDatabaseService;

/**
 * 基于 database-api 的鉴权账户仓储适配器。
 * 职责：在 auth feature 内完成领域账户模型与 database-api 契约模型之间的转换。
 * 边界：不包含 SQL 与数据库驱动细节，具体持久化由 database-impl 提供。
 */
public class DatabaseBackedAuthAccountRepository implements AuthAccountRepository {

    private final AuthAccountDatabaseService authAccountDatabaseService;

    public DatabaseBackedAuthAccountRepository(AuthAccountDatabaseService authAccountDatabaseService) {
        this.authAccountDatabaseService = authAccountDatabaseService;
    }

    /**
     * 按用户名查询鉴权账户。
     * 输出：存在时返回领域账户，不存在时返回空。
     */
    @Override
    public Optional<AuthAccount> findByUsername(String username) {
        return authAccountDatabaseService.findByUsername(username)
                .map(this::toDomainModel);
    }

    /**
     * 按账户 ID 查询鉴权账户。
     */
    @Override
    public Optional<AuthAccount> findById(long accountId) {
        return authAccountDatabaseService.findById(accountId)
                .map(this::toDomainModel);
    }

    /**
     * 持久化一个新的鉴权账户。
     * 副作用：会把用户名和密码散列写入持久层。
     */
    @Override
    public AuthAccount save(AuthAccount account) {
        authAccountDatabaseService.insert(toWriteRecord(account));
        return account;
    }

    /**
     * 更新既有鉴权账户。
     */
    @Override
    public AuthAccount update(AuthAccount account) {
        authAccountDatabaseService.update(toWriteRecord(account));
        return account;
    }

    private AuthAccount toDomainModel(AuthAccountRecord record) {
        return new AuthAccount(
                record.id(),
                record.username(),
                record.passwordHash(),
                record.createdAt(),
                record.updatedAt()
        );
    }

    private AuthAccountRecord toWriteRecord(AuthAccount account) {
        return new AuthAccountRecord(
                account.id(),
                account.username(),
                account.passwordHash(),
                account.createdAt(),
                account.updatedAt()
        );
    }
}
