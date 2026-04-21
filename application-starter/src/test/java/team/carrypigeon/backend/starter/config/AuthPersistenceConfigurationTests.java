package team.carrypigeon.backend.starter.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import team.carrypigeon.backend.chat.domain.features.auth.domain.repository.AuthAccountRepository;
import team.carrypigeon.backend.chat.domain.features.auth.domain.repository.AuthRefreshSessionRepository;
import team.carrypigeon.backend.infrastructure.service.database.impl.config.DatabaseServiceAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * AuthPersistenceConfiguration 装配测试。
 * 职责：验证 starter 层是否将 auth 仓储抽象装配到 database-api/database-impl 提供的数据库服务能力。
 * 边界：不访问真实数据库，只验证 Spring Bean 装配边界。
 */
class AuthPersistenceConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    DatabaseServiceAutoConfiguration.class,
                    AuthPersistenceConfiguration.class
            ));

    /**
     * 验证开启数据库服务时会注册 auth 仓储适配器 Bean。
     * 输入：数据库服务开关、JdbcClient 与事务管理器。
     * 输出：starter 层存在 auth 领域仓储抽象 Bean。
     */
    @Test
    @DisplayName("configuration with database service registers auth repositories")
    void configuration_withDatabaseService_registersAuthRepositories() {
        contextRunner
                .withPropertyValues(
                        "cp.infrastructure.service.database.enabled=true",
                        "cp.infrastructure.service.database.health-query=SELECT 1"
                )
                .withBean(JdbcClient.class, () -> mock(JdbcClient.class))
                .withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(AuthAccountRepository.class);
                    assertThat(context).hasSingleBean(AuthRefreshSessionRepository.class);
                });
    }

    /**
     * 验证关闭数据库服务时不会注册 auth 仓储适配器 Bean。
     * 输入：关闭数据库服务并提供数据库相关基础 Bean。
     * 输出：starter 层不存在 auth 领域仓储抽象 Bean。
     */
    @Test
    @DisplayName("configuration without database service does not register auth repositories")
    void configuration_withoutDatabaseService_doesNotRegisterAuthRepositories() {
        contextRunner
                .withPropertyValues("cp.infrastructure.service.database.enabled=false")
                .withBean(JdbcClient.class, () -> mock(JdbcClient.class))
                .withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(AuthAccountRepository.class);
                    assertThat(context).doesNotHaveBean(AuthRefreshSessionRepository.class);
                });
    }
}
