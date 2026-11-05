package team.carrypigeon.backend.infrastructure.service.database.impl.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.simple.JdbcClient;
import team.carrypigeon.backend.infrastructure.basic.startup.InitializationChecker;
import team.carrypigeon.backend.infrastructure.service.database.api.health.DatabaseHealthService;
import team.carrypigeon.backend.infrastructure.service.database.impl.health.JdbcDatabaseHealthService;
import team.carrypigeon.backend.infrastructure.service.database.impl.jdbc.JdbcClientSupport;
import team.carrypigeon.backend.infrastructure.service.database.impl.startup.DatabaseInitializationChecker;

/**
 * 数据库服务自动配置。
 * 职责：在 database-impl 内装配 MySQL 数据库实现。
 * 边界：只创建具体实现 Bean，不在 API 模块中声明 Spring 装配逻辑。
 */
@AutoConfiguration
@EnableConfigurationProperties(DatabaseServiceProperties.class)
@MapperScan(
    basePackages = "team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper",
    annotationClass = org.apache.ibatis.annotations.Mapper.class
)
public class DatabaseServiceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DataSource dataSource(DatabaseServiceProperties properties) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(properties.url());
        dataSource.setUsername(properties.username());
        dataSource.setPassword(properties.password());
        dataSource.setDriverClassName(properties.driverClassName());
        dataSource.setConnectionTimeout(properties.connectionTimeout().toMillis());
        dataSource.setValidationTimeout(properties.validationTimeout().toMillis());
        return dataSource;
    }

    @Bean
    @ConditionalOnMissingBean
    public JdbcClient jdbcClient(DataSource dataSource) {
        return JdbcClient.create(dataSource);
    }

    @Bean
    @ConditionalOnMissingBean
    public JdbcClientSupport jdbcClientSupport(JdbcClient jdbcClient) {
        return new JdbcClientSupport(jdbcClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public DatabaseHealthService databaseHealthService(JdbcClientSupport jdbcClientSupport) {
        return new JdbcDatabaseHealthService(jdbcClientSupport);
    }

    @Bean
    @ConditionalOnMissingBean(name = "databaseInitializationCheck")
    public InitializationChecker databaseInitializationCheck(DatabaseHealthService databaseHealthService) {
        return new DatabaseInitializationChecker(databaseHealthService);
    }

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
