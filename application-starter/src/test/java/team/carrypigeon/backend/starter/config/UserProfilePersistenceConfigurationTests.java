package team.carrypigeon.backend.starter.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import team.carrypigeon.backend.chat.domain.features.user.config.UserProfilePersistenceConfiguration;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
import team.carrypigeon.backend.infrastructure.service.database.impl.config.DatabaseServiceAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * UserProfilePersistenceConfiguration 装配测试。
 * 职责：验证 starter 运行时是否能发现 chat-domain 中的 user 仓储装配配置。
 * 边界：不访问真实数据库，只验证 Spring Bean 装配边界。
 */
class UserProfilePersistenceConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DatabaseServiceAutoConfiguration.class))
            .withUserConfiguration(UserProfilePersistenceConfiguration.class);

    /**
     * 验证开启数据库服务时会注册用户资料仓储适配器 Bean。
     * 输入：数据库服务开关、JdbcClient 与事务管理器。
     * 输出：运行时存在用户资料仓储抽象 Bean。
     */
    @Test
    @DisplayName("configuration with database service registers user profile repository")
    void configuration_withDatabaseService_registersUserProfileRepository() {
        contextRunner
                .withPropertyValues(
                        "cp.infrastructure.service.database.enabled=true",
                        "cp.infrastructure.service.database.health-query=SELECT 1"
                )
                .withBean(JdbcClient.class, () -> mock(JdbcClient.class))
                .withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class))
                .run(context -> assertThat(context).hasSingleBean(UserProfileRepository.class));
    }

    /**
     * 验证关闭数据库服务时不会注册用户资料仓储适配器 Bean。
     * 输入：关闭数据库服务并提供数据库相关基础 Bean。
     * 输出：运行时不存在用户资料仓储抽象 Bean。
     */
    @Test
    @DisplayName("configuration without database service does not register user profile repository")
    void configuration_withoutDatabaseService_doesNotRegisterUserProfileRepository() {
        contextRunner
                .withPropertyValues("cp.infrastructure.service.database.enabled=false")
                .withBean(JdbcClient.class, () -> mock(JdbcClient.class))
                .withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class))
                .run(context -> assertThat(context).doesNotHaveBean(UserProfileRepository.class));
    }
}
