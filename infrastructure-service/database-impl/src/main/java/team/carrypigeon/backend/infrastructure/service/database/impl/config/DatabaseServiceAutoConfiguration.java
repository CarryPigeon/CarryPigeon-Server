package team.carrypigeon.backend.infrastructure.service.database.impl.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import team.carrypigeon.backend.infrastructure.service.database.api.health.DatabaseHealthService;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;
import team.carrypigeon.backend.infrastructure.service.database.impl.health.JdbcDatabaseHealthService;
import team.carrypigeon.backend.infrastructure.service.database.impl.jdbc.JdbcClientSupport;
import team.carrypigeon.backend.infrastructure.service.database.impl.transaction.SpringTransactionRunner;

/**
 * 数据库服务自动配置。
 * 职责：在 database-impl 内装配 JDBC 数据库服务实现。
 * 边界：只创建具体实现 Bean，不向 database-api 写入任何 Spring 装配逻辑。
 */
@AutoConfiguration
@EnableConfigurationProperties(DatabaseServiceProperties.class)
@ConditionalOnProperty(prefix = "cp.infrastructure.service.database", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseServiceAutoConfiguration {

    /**
     * 创建 JDBC 支持入口。
     *
     * @param jdbcClient Spring JDBC 客户端
     * @return JDBC 支持入口
     */
    @Bean
    @ConditionalOnMissingBean
    public JdbcClientSupport jdbcClientSupport(JdbcClient jdbcClient) {
        return new JdbcClientSupport(jdbcClient);
    }

    /**
     * 创建数据库健康检查服务。
     *
     * @param jdbcClientSupport JDBC 支持入口
     * @param properties 数据库服务配置
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
     * 创建事务运行器。
     *
     * @param transactionManager Spring 事务管理器
     * @return 数据库事务运行器
     */
    @Bean
    @ConditionalOnMissingBean
    public TransactionRunner transactionRunner(PlatformTransactionManager transactionManager) {
        return new SpringTransactionRunner(new TransactionTemplate(transactionManager));
    }
}
