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
import team.carrypigeon.backend.infrastructure.service.database.impl.persistence.file.FileUploadDatabaseServiceImpl;
import team.carrypigeon.backend.infrastructure.service.database.impl.persistence.auth.account.AuthAccountDatabaseServiceImpl;
import team.carrypigeon.backend.infrastructure.service.database.impl.persistence.auth.session.AuthRefreshSessionDatabaseServiceImpl;
import team.carrypigeon.backend.infrastructure.service.database.impl.persistence.channel.application.ChannelApplicationDatabaseServiceImpl;
import team.carrypigeon.backend.infrastructure.service.database.impl.persistence.channel.ban.ChannelBanDatabaseServiceImpl;
import team.carrypigeon.backend.infrastructure.service.database.impl.persistence.channel.ChannelDatabaseServiceImpl;
import team.carrypigeon.backend.infrastructure.service.database.impl.persistence.channel.member.ChannelMemberDatabaseServiceImpl;
import team.carrypigeon.backend.infrastructure.service.database.impl.persistence.channel.readstate.ChannelReadStateDatabaseServiceImpl;
import team.carrypigeon.backend.infrastructure.service.database.impl.persistence.message.mention.MentionDatabaseServiceImpl;
import team.carrypigeon.backend.infrastructure.service.database.impl.persistence.message.MessageDatabaseServiceImpl;
import team.carrypigeon.backend.infrastructure.service.database.impl.persistence.user.UserProfileDatabaseServiceImpl;
import team.carrypigeon.backend.infrastructure.service.database.impl.persistence.auth.account.AuthAccountMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.persistence.auth.session.AuthRefreshSessionMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.persistence.channel.application.ChannelApplicationMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.persistence.channel.ban.ChannelBanMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.persistence.channel.ChannelMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.persistence.channel.member.ChannelMemberMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.persistence.channel.readstate.ChannelReadStateMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.persistence.file.FileUploadMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.persistence.message.mention.MentionMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.persistence.message.MessageMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.persistence.user.UserProfileMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.simple.JdbcClient;
import team.carrypigeon.backend.infrastructure.service.cache.api.service.CacheService;
import team.carrypigeon.backend.infrastructure.service.database.api.health.DatabaseHealthService;
import team.carrypigeon.backend.infrastructure.service.database.impl.health.MysqlInitializationChecker;
import team.carrypigeon.backend.infrastructure.service.database.impl.cache.DatabaseCache;
import team.carrypigeon.backend.infrastructure.service.database.impl.health.JdbcDatabaseHealthService;

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
    basePackages = "team.carrypigeon.backend.infrastructure.service.database.impl.persistence",
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
    public DatabaseHealthService databaseHealthService(JdbcClient jdbcClient) {
        return new JdbcDatabaseHealthService(jdbcClient);
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

    @Bean
    @ConditionalOnMissingBean(FileUploadDatabaseService.class)
    public FileUploadDatabaseService fileUploadDatabaseService(FileUploadMapper mapper) {
        return new FileUploadDatabaseServiceImpl(mapper);
    }

    @Bean
    @ConditionalOnMissingBean(AuthAccountDatabaseService.class)
    public AuthAccountDatabaseService authAccountDatabaseService(AuthAccountMapper authAccountMapper, DatabaseCache databaseCache) {
        return new AuthAccountDatabaseServiceImpl(authAccountMapper, databaseCache);
    }

    @Bean
    @ConditionalOnMissingBean(AuthRefreshSessionDatabaseService.class)
    public AuthRefreshSessionDatabaseService authRefreshSessionDatabaseService(AuthRefreshSessionMapper authRefreshSessionMapper, TimeProvider timeProvider) {
        return new AuthRefreshSessionDatabaseServiceImpl(authRefreshSessionMapper, timeProvider);
    }

    @Bean
    @ConditionalOnMissingBean(ChannelApplicationDatabaseService.class)
    public ChannelApplicationDatabaseService channelApplicationDatabaseService(ChannelApplicationMapper channelApplicationMapper) {
        return new ChannelApplicationDatabaseServiceImpl(channelApplicationMapper);
    }

    @Bean
    @ConditionalOnMissingBean(ChannelBanDatabaseService.class)
    public ChannelBanDatabaseService channelBanDatabaseService(ChannelBanMapper channelBanMapper, DatabaseCache databaseCache) {
        return new ChannelBanDatabaseServiceImpl(channelBanMapper, databaseCache);
    }

    @Bean
    @ConditionalOnMissingBean(ChannelDatabaseService.class)
    public ChannelDatabaseService channelDatabaseService(ChannelMapper channelMapper, DatabaseCache databaseCache) {
        return new ChannelDatabaseServiceImpl(channelMapper, databaseCache);
    }

    @Bean
    @ConditionalOnMissingBean(ChannelMemberDatabaseService.class)
    public ChannelMemberDatabaseService channelMemberDatabaseService(ChannelMemberMapper channelMemberMapper, DatabaseCache databaseCache) {
        return new ChannelMemberDatabaseServiceImpl(channelMemberMapper, databaseCache);
    }

    @Bean
    @ConditionalOnMissingBean(ChannelReadStateDatabaseService.class)
    public ChannelReadStateDatabaseService channelReadStateDatabaseService(ChannelReadStateMapper channelReadStateMapper) {
        return new ChannelReadStateDatabaseServiceImpl(channelReadStateMapper);
    }

    @Bean
    @ConditionalOnMissingBean(MentionDatabaseService.class)
    public MentionDatabaseService mentionDatabaseService(MentionMapper mentionMapper) {
        return new MentionDatabaseServiceImpl(mentionMapper);
    }


    @Bean
    @ConditionalOnMissingBean(MessageDatabaseService.class)
    public MessageDatabaseService messageDatabaseService(MessageMapper messageMapper) {
        return new MessageDatabaseServiceImpl(messageMapper);
    }

    @Bean
    @ConditionalOnMissingBean(UserProfileDatabaseService.class)
    public UserProfileDatabaseService userProfileDatabaseService(UserProfileMapper userProfileMapper, DatabaseCache databaseCache) {
        return new UserProfileDatabaseServiceImpl(userProfileMapper, databaseCache);
    }
    /** 为已启用的Database实现注册独立启动检查。 */
    @Bean
    @ConditionalOnMissingBean(MysqlInitializationChecker.class)
    public MysqlInitializationChecker databaseInitializationChecker(DatabaseHealthService healthService) {
        return new MysqlInitializationChecker(healthService);
    }

}
