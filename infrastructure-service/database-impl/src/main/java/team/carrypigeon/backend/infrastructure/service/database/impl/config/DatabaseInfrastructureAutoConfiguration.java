package team.carrypigeon.backend.infrastructure.service.database.impl.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import team.carrypigeon.backend.infrastructure.basic.startup.InitializationCheck;
import team.carrypigeon.backend.infrastructure.service.database.api.health.DatabaseHealthService;
import team.carrypigeon.backend.infrastructure.service.database.api.service.PluginMigrationDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;
import team.carrypigeon.backend.infrastructure.service.database.impl.health.JdbcDatabaseHealthService;
import team.carrypigeon.backend.infrastructure.service.database.impl.jdbc.JdbcClientSupport;
import team.carrypigeon.backend.infrastructure.service.database.impl.jdbc.JdbcPluginMigrationDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.startup.DatabaseInitializationCheck;
import team.carrypigeon.backend.infrastructure.service.database.impl.transaction.SpringTransactionRunner;

/**
 * 数据库基础设施自动配置。
 * 职责：装配 database-impl 内部共享的 JDBC、健康检查、初始化检查与事务能力。
 * 边界：不装配具体业务数据库服务 Bean。
 */
@AutoConfiguration(afterName = "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration")
@EnableConfigurationProperties(DatabaseServiceProperties.class)
@ConditionalOnProperty(prefix = "cp.infrastructure.service.database", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseInfrastructureAutoConfiguration {

    /**
     * 装配 JDBC 访问支持对象。
     *
     * @param jdbcClient Spring JDBC 客户端
     * @return database-impl 内部共用的 JDBC 支持对象
     */
    @Bean
    @ConditionalOnMissingBean
    public JdbcClientSupport jdbcClientSupport(JdbcClient jdbcClient) {
        return new JdbcClientSupport(jdbcClient);
    }

    /**
     * 装配数据库健康检查服务。
     *
     * @param jdbcClientSupport JDBC 支持对象
     * @param properties 数据库实现配置
     * @return 数据库健康检查服务
     */
    @Bean
    @ConditionalOnMissingBean
    public DatabaseHealthService databaseHealthService(
            JdbcClientSupport jdbcClientSupport,
            DatabaseServiceProperties properties
    ) {
        return new JdbcDatabaseHealthService(jdbcClientSupport, properties);
    }

    /**
     * 装配数据库初始化检查。
     * 副作用：在启动阶段用于阻断数据库不可用的运行环境。
     *
     * @param databaseHealthService 数据库健康检查服务
     * @return 启动初始化检查实例
     */
    @Bean
    @ConditionalOnMissingBean(name = "databaseInitializationCheck")
    public InitializationCheck databaseInitializationCheck(DatabaseHealthService databaseHealthService) {
        return new DatabaseInitializationCheck(databaseHealthService);
    }

    /**
     * 装配事务执行器。
     *
     * @param transactionManager Spring 事务管理器
     * @return 供 domain/application 使用的事务执行适配器
     */
    @Bean
    @ConditionalOnMissingBean
    public TransactionRunner transactionRunner(PlatformTransactionManager transactionManager) {
        return new SpringTransactionRunner(new TransactionTemplate(transactionManager));
    }

    /**
     * 装配插件迁移历史数据库服务。
     *
     * @param jdbcClient Spring JDBC 客户端
     * @return 插件迁移历史服务
     */
    @Bean
    @ConditionalOnMissingBean
    public PluginMigrationDatabaseService pluginMigrationDatabaseService(JdbcClient jdbcClient) {
        return new JdbcPluginMigrationDatabaseService(jdbcClient);
    }
}
