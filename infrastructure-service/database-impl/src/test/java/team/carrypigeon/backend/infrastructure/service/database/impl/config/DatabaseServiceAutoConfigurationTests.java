package team.carrypigeon.backend.infrastructure.service.database.impl.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import team.carrypigeon.backend.infrastructure.service.database.api.health.DatabaseHealthService;
import team.carrypigeon.backend.infrastructure.service.database.api.service.AuthAccountDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.api.service.AuthRefreshSessionDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;
import team.carrypigeon.backend.infrastructure.service.database.impl.jdbc.JdbcClientSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 验证数据库自动配置的装配边界。
 * 职责：确保 database-impl 只在启用条件满足时注册数据库服务相关 Bean。
 * 边界：不访问真实数据库，只验证自动配置的上下文装配行为。
 */
class DatabaseServiceAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DatabaseServiceAutoConfiguration.class));

    /**
     * 测试启用数据库服务时的自动配置。
     * 输入：启用开关、健康检查 SQL、JdbcClient 与事务管理器。
     * 期望：成功装配数据库健康检查、事务运行器和 JDBC 支持入口。
     */
    @Test
    void autoConfiguration_enabled_registersDatabaseBeans() {
        contextRunner
                .withPropertyValues(
                        "cp.infrastructure.service.database.enabled=true",
                        "cp.infrastructure.service.database.health-query=SELECT 1"
                )
                .withBean(JdbcClient.class, () -> mock(JdbcClient.class))
                .withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class))
                 .run(context -> {
                    assertThat(context).hasSingleBean(AuthAccountDatabaseService.class);
                    assertThat(context).hasSingleBean(AuthRefreshSessionDatabaseService.class);
                    assertThat(context).hasSingleBean(DatabaseHealthService.class);
                     assertThat(context).hasSingleBean(TransactionRunner.class);
                     assertThat(context).hasSingleBean(JdbcClientSupport.class);
                });
    }

    /**
     * 测试禁用数据库服务时的自动配置。
     * 输入：禁用开关以及数据库相关基础 Bean。
     * 期望：database-impl 不注册数据库服务相关 Bean。
     */
    @Test
    void autoConfiguration_disabled_skipsDatabaseBeans() {
        contextRunner
                .withPropertyValues("cp.infrastructure.service.database.enabled=false")
                .withBean(JdbcClient.class, () -> mock(JdbcClient.class))
                .withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class))
                 .run(context -> {
                    assertThat(context).doesNotHaveBean(AuthAccountDatabaseService.class);
                    assertThat(context).doesNotHaveBean(AuthRefreshSessionDatabaseService.class);
                    assertThat(context).doesNotHaveBean(DatabaseHealthService.class);
                     assertThat(context).doesNotHaveBean(TransactionRunner.class);
                     assertThat(context).doesNotHaveBean(JdbcClientSupport.class);
                 });
    }
}
