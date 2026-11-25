package team.carrypigeon.backend.infrastructure.service.database.impl.config;

import static team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent.DEV;
import static team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent.PROD;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent;
import team.carrypigeon.backend.infrastructure.service.cache.api.service.CacheService;
import team.carrypigeon.backend.infrastructure.service.database.impl.cache.DatabaseCache;

/** 配置 MySQL 数据源、数据库缓存和 MyBatis Mapper 扫描，持久化服务由组件扫描注册。 */
@Configuration(proxyBeanMethods = false)
@MapperScan(
    basePackages = "team.carrypigeon.backend.infrastructure.service.database.impl.persistence",
    annotationClass = org.apache.ibatis.annotations.Mapper.class
)
@EnvironmentComponent(profiles = {PROD, DEV})
public class DatabaseServiceConfiguration {

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

    /**
     * 创建数据库查询缓存门面。
     * 说明：使用 {@link ObjectProvider} 延迟解析缓存实现，数据库缓存门面可独立退化为直读；完整应用仍要求缓存能力及其健康检查可用。
     *
     * @param cacheServiceProvider 字符串缓存实现提供者
     * @param objectMapper 项目统一 ObjectMapper
     * @param properties 数据库服务配置（含缓存 TTL 与批量上限）
     * @return 数据库查询缓存门面
     */
    @Bean
    @ConditionalOnMissingBean
    public DatabaseCache databaseCache(
            ObjectProvider<CacheService> cacheServiceProvider,
            ObjectMapper objectMapper,
            DatabaseServiceProperties properties
    ) {
        return new DatabaseCache(cacheServiceProvider.getIfAvailable(), objectMapper, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
