package team.carrypigeon.backend.starter.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import team.carrypigeon.backend.chat.domain.features.auth.domain.repository.AuthAccountRepository;
import team.carrypigeon.backend.chat.domain.features.auth.domain.repository.AuthRefreshSessionRepository;
import team.carrypigeon.backend.infrastructure.service.database.api.service.AuthAccountDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.api.service.AuthRefreshSessionDatabaseService;

/**
 * 鉴权持久化装配配置。
 * 职责：在 application-starter 中装配 auth 领域仓储抽象到数据库服务契约之间的运行时适配器。
 * 边界：这里只负责运行时 Bean 装配，不承载鉴权业务规则，也不承载 JDBC 实现细节。
 */
@Configuration
public class AuthPersistenceConfiguration {

    /**
     * 创建鉴权账户仓储适配器。
     *
     * @param authAccountDatabaseService 鉴权账户数据库服务契约
     * @return 面向 chat-domain 的账户仓储实现
     */
    @Bean
    @ConditionalOnBean(AuthAccountDatabaseService.class)
    public AuthAccountRepository authAccountRepository(AuthAccountDatabaseService authAccountDatabaseService) {
        return new StarterAuthAccountRepository(authAccountDatabaseService);
    }

    /**
     * 创建刷新会话仓储适配器。
     *
     * @param authRefreshSessionDatabaseService 刷新会话数据库服务契约
     * @return 面向 chat-domain 的刷新会话仓储实现
     */
    @Bean
    @ConditionalOnBean(AuthRefreshSessionDatabaseService.class)
    public AuthRefreshSessionRepository authRefreshSessionRepository(
            AuthRefreshSessionDatabaseService authRefreshSessionDatabaseService
    ) {
        return new StarterAuthRefreshSessionRepository(authRefreshSessionDatabaseService);
    }
}
