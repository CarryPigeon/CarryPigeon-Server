package team.carrypigeon.backend.starter.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import team.carrypigeon.backend.chat.domain.features.auth.config.AuthPersistenceConfiguration;
import team.carrypigeon.backend.chat.domain.features.auth.domain.repository.AuthAccountRepository;
import team.carrypigeon.backend.chat.domain.features.auth.domain.repository.AuthRefreshSessionRepository;
import team.carrypigeon.backend.infrastructure.service.database.api.service.AuthAccountDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.api.service.AuthRefreshSessionDatabaseService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * AuthPersistenceConfiguration 装配测试。
 * 职责：验证 starter 运行时是否能发现 chat-domain 中的 auth 仓储装配配置。
 * 边界：不访问真实数据库，只验证 Spring Bean 装配边界。
 */
@Tag("contract")
class AuthPersistenceConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AuthPersistenceConfiguration.class);

    /**
     * 验证开启数据库服务时会注册 auth 仓储适配器 Bean。
     * 输入：数据库服务开关与 database-api 服务契约 mock。
     * 输出：运行时存在 auth 领域仓储抽象 Bean。
     */
    @Test
    @DisplayName("configuration with database service registers auth repositories")
    void configuration_withDatabaseService_registersAuthRepositories() {
        contextRunner
                .withPropertyValues(
                        "cp.infrastructure.service.database.enabled=true",
                        "cp.infrastructure.service.database.health-query=SELECT 1"
                )
                .withBean(AuthAccountDatabaseService.class, () -> mock(AuthAccountDatabaseService.class))
                .withBean(AuthRefreshSessionDatabaseService.class, () -> mock(AuthRefreshSessionDatabaseService.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(AuthAccountRepository.class);
                    assertThat(context).hasSingleBean(AuthRefreshSessionRepository.class);
                });
    }

    /**
     * 验证关闭数据库服务时不会注册 auth 仓储适配器 Bean。
     * 输入：关闭数据库服务并提供数据库相关基础 Bean。
     * 输出：运行时不存在 auth 领域仓储抽象 Bean。
     */
    @Test
    @DisplayName("configuration without database service does not register auth repositories")
    void configuration_withoutDatabaseService_doesNotRegisterAuthRepositories() {
        contextRunner
                .withPropertyValues("cp.infrastructure.service.database.enabled=false")
                .withBean(AuthAccountDatabaseService.class, () -> mock(AuthAccountDatabaseService.class))
                .withBean(AuthRefreshSessionDatabaseService.class, () -> mock(AuthRefreshSessionDatabaseService.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(AuthAccountRepository.class);
                    assertThat(context).doesNotHaveBean(AuthRefreshSessionRepository.class);
                });
    }
}
