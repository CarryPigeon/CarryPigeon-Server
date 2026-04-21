package team.carrypigeon.backend.chat.domain.features.auth.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import team.carrypigeon.backend.chat.domain.features.auth.domain.repository.AuthAccountRepository;
import team.carrypigeon.backend.chat.domain.features.auth.domain.repository.AuthRefreshSessionRepository;
import team.carrypigeon.backend.chat.domain.features.auth.support.persistence.DatabaseBackedAuthAccountRepository;
import team.carrypigeon.backend.chat.domain.features.auth.support.persistence.DatabaseBackedAuthRefreshSessionRepository;
import team.carrypigeon.backend.infrastructure.service.database.api.service.AuthAccountDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.api.service.AuthRefreshSessionDatabaseService;

/**
 * 鉴权持久化装配配置。
 * 职责：在 auth feature 内装配领域仓储抽象到 database-api 服务契约之间的运行时适配器。
 * 边界：这里只负责 Bean 装配，不承载鉴权业务规则，也不承载 JDBC 实现细节。
 */
@Configuration
@ConditionalOnProperty(prefix = "cp.infrastructure.service.database", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AuthPersistenceConfiguration {

    /**
     * 创建鉴权账户仓储适配器。
     *
     * @param authAccountDatabaseService 鉴权账户数据库服务契约
     * @return 面向 auth 领域的账户仓储实现
     */
    @Bean
    public AuthAccountRepository authAccountRepository(AuthAccountDatabaseService authAccountDatabaseService) {
        return new DatabaseBackedAuthAccountRepository(authAccountDatabaseService);
    }

    /**
     * 创建刷新会话仓储适配器。
     *
     * @param authRefreshSessionDatabaseService 刷新会话数据库服务契约
     * @return 面向 auth 领域的刷新会话仓储实现
     */
    @Bean
    public AuthRefreshSessionRepository authRefreshSessionRepository(
            AuthRefreshSessionDatabaseService authRefreshSessionDatabaseService
    ) {
        return new DatabaseBackedAuthRefreshSessionRepository(authRefreshSessionDatabaseService);
    }
}
