package team.carrypigeon.backend.infrastructure.service.database.impl.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import team.carrypigeon.backend.infrastructure.service.database.api.service.AuthAccountDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.api.service.AuthRefreshSessionDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelApplicationDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelBanDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelMemberDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelReadStateDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.api.service.FileUploadDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.api.service.MentionDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.api.service.MessageDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.api.service.UserProfileDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service.MybatisFileUploadDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service.MybatisPlusAuthAccountDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service.MybatisPlusAuthRefreshSessionDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service.MybatisPlusChannelApplicationDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service.MybatisPlusChannelBanDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service.MybatisPlusChannelDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service.MybatisPlusChannelMemberDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service.MybatisPlusChannelReadStateDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service.MybatisPlusMentionDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service.MybatisPlusMessageDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service.MybatisPlusUserProfileDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.AuthAccountMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.AuthRefreshSessionMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.ChannelApplicationMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.ChannelBanMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.ChannelMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.ChannelMemberMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.ChannelReadStateMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.FileUploadMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.MentionMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.MessageMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.UserProfileMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.cache.CacheInvalidationTasks;
import org.springframework.jdbc.core.simple.JdbcClient;
import team.carrypigeon.backend.infrastructure.service.cache.api.service.CacheService;
import team.carrypigeon.backend.infrastructure.service.database.api.health.DatabaseHealthService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.cache.DatabaseCache;
import team.carrypigeon.backend.infrastructure.service.database.impl.health.JdbcDatabaseHealthService;
import team.carrypigeon.backend.infrastructure.service.database.impl.jdbc.JdbcClientSupport;

/**
 * 数据库服务自动配置。
 * 职责：在 database-impl 内装配 MySQL 数据库实现。
 * 边界：只创建具体实现 Bean，不在 API 模块中声明 Spring 装配逻辑。
 */
@AutoConfiguration(beforeName = {"org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
        "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration"})
@ConditionalOnProperty(prefix = "service.database.mysql", name = "enabled", havingValue = "true", matchIfMissing = true)
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
            DatabaseServiceProperties properties,
            CacheInvalidationTasks invalidationTasks
    ) {
        return new DatabaseCache(cacheServiceProvider.getIfAvailable(), objectMapper, properties, invalidationTasks);
    }

    /** 失效任务属于数据库适配实现，业务模块无需知道缓存与重试的存储结构。 */
    @Bean
    public CacheInvalidationTasks cacheInvalidationTasks(JdbcClient jdbc, PlatformTransactionManager transactions,
            ObjectProvider<CacheService> cache) {
        return new CacheInvalidationTasks(jdbc, transactions, cache.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Bean
    @ConditionalOnMissingBean(FileUploadDatabaseService.class)
    public FileUploadDatabaseService fileUploadDatabaseService(FileUploadMapper mapper) {
        return new MybatisFileUploadDatabaseService(mapper);
    }

    @Bean
    @ConditionalOnMissingBean(AuthAccountDatabaseService.class)
    public AuthAccountDatabaseService authAccountDatabaseService(AuthAccountMapper authAccountMapper, DatabaseCache databaseCache) {
        return new MybatisPlusAuthAccountDatabaseService(authAccountMapper, databaseCache);
    }

    @Bean
    @ConditionalOnMissingBean(AuthRefreshSessionDatabaseService.class)
    public AuthRefreshSessionDatabaseService authRefreshSessionDatabaseService(AuthRefreshSessionMapper authRefreshSessionMapper, TimeProvider timeProvider) {
        return new MybatisPlusAuthRefreshSessionDatabaseService(authRefreshSessionMapper, timeProvider);
    }

    @Bean
    @ConditionalOnMissingBean(ChannelApplicationDatabaseService.class)
    public ChannelApplicationDatabaseService channelApplicationDatabaseService(ChannelApplicationMapper channelApplicationMapper) {
        return new MybatisPlusChannelApplicationDatabaseService(channelApplicationMapper);
    }

    @Bean
    @ConditionalOnMissingBean(ChannelBanDatabaseService.class)
    public ChannelBanDatabaseService channelBanDatabaseService(ChannelBanMapper channelBanMapper, DatabaseCache databaseCache) {
        return new MybatisPlusChannelBanDatabaseService(channelBanMapper, databaseCache);
    }

    @Bean
    @ConditionalOnMissingBean(ChannelDatabaseService.class)
    public ChannelDatabaseService channelDatabaseService(ChannelMapper channelMapper, DatabaseCache databaseCache) {
        return new MybatisPlusChannelDatabaseService(channelMapper, databaseCache);
    }

    @Bean
    @ConditionalOnMissingBean(ChannelMemberDatabaseService.class)
    public ChannelMemberDatabaseService channelMemberDatabaseService(ChannelMemberMapper channelMemberMapper, DatabaseCache databaseCache) {
        return new MybatisPlusChannelMemberDatabaseService(channelMemberMapper, databaseCache);
    }

    @Bean
    @ConditionalOnMissingBean(ChannelReadStateDatabaseService.class)
    public ChannelReadStateDatabaseService channelReadStateDatabaseService(ChannelReadStateMapper channelReadStateMapper) {
        return new MybatisPlusChannelReadStateDatabaseService(channelReadStateMapper);
    }

    @Bean
    @ConditionalOnMissingBean(MentionDatabaseService.class)
    public MentionDatabaseService mentionDatabaseService(MentionMapper mentionMapper) {
        return new MybatisPlusMentionDatabaseService(mentionMapper);
    }

    @Bean
    @ConditionalOnMissingBean(MessageDatabaseService.class)
    public MessageDatabaseService messageDatabaseService(MessageMapper messageMapper) {
        return new MybatisPlusMessageDatabaseService(messageMapper);
    }

    @Bean
    @ConditionalOnMissingBean(UserProfileDatabaseService.class)
    public UserProfileDatabaseService userProfileDatabaseService(UserProfileMapper userProfileMapper, DatabaseCache databaseCache) {
        return new MybatisPlusUserProfileDatabaseService(userProfileMapper, databaseCache);
    }
}
