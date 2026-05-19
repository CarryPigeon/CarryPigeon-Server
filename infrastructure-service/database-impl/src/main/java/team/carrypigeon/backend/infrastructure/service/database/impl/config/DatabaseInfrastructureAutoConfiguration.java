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
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;
import team.carrypigeon.backend.infrastructure.service.database.impl.health.JdbcDatabaseHealthService;
import team.carrypigeon.backend.infrastructure.service.database.impl.jdbc.JdbcClientSupport;
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

    @Bean
    @ConditionalOnMissingBean
    public JdbcClientSupport jdbcClientSupport(JdbcClient jdbcClient) {
        return new JdbcClientSupport(jdbcClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public DatabaseHealthService databaseHealthService(
            JdbcClientSupport jdbcClientSupport,
            DatabaseServiceProperties properties
    ) {
        return new JdbcDatabaseHealthService(jdbcClientSupport, properties);
    }

    @Bean
    @ConditionalOnMissingBean(name = "databaseInitializationCheck")
    public InitializationCheck databaseInitializationCheck(DatabaseHealthService databaseHealthService) {
        return new DatabaseInitializationCheck(databaseHealthService);
    }

    @Bean
    @ConditionalOnMissingBean
    public TransactionRunner transactionRunner(PlatformTransactionManager transactionManager) {
        return new SpringTransactionRunner(new TransactionTemplate(transactionManager));
    }
}
