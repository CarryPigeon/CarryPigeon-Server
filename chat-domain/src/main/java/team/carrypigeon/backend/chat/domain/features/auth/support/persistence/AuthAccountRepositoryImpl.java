package team.carrypigeon.backend.chat.domain.features.auth.support.persistence;

import java.util.Optional;
import org.springframework.stereotype.Repository;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthAccount;
import team.carrypigeon.backend.chat.domain.features.auth.domain.repository.AuthAccountRepository;
import team.carrypigeon.backend.infrastructure.service.database.api.model.AuthAccountRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.AuthAccountDatabaseService;

/**
 * 鉴权账户仓储实现。
 * 职责：在 chat-domain 内完成领域模型与 database-api 契约模型的转换。
 * 边界：不包含 SQL 与数据库驱动细节，具体持久化由 database-impl 承担。
 */
@Repository
public class AuthAccountRepositoryImpl implements AuthAccountRepository {

    private final AuthAccountDatabaseService authAccountDatabaseService;

    public AuthAccountRepositoryImpl(AuthAccountDatabaseService authAccountDatabaseService) {
        this.authAccountDatabaseService = authAccountDatabaseService;
    }

    @Override
    public Optional<AuthAccount> findByUsername(String username) {
        return authAccountDatabaseService.findByUsername(username)
                .map(this::toDomainModel);
    }

    @Override
    public AuthAccount save(AuthAccount account) {
        authAccountDatabaseService.insert(toRecord(account));
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

    private AuthAccountRecord toRecord(AuthAccount account) {
        return new AuthAccountRecord(
                account.id(),
                account.username(),
                account.passwordHash(),
                account.createdAt(),
                account.updatedAt()
        );
    }
}
