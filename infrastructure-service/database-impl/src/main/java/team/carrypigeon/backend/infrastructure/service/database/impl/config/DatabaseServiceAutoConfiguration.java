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
import team.carrypigeon.backend.infrastructure.service.database.api.service.AuthAccountDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.api.service.AuthRefreshSessionDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelMemberDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.api.service.MessageDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.api.service.UserProfileDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;
import team.carrypigeon.backend.infrastructure.service.database.impl.health.JdbcDatabaseHealthService;
import team.carrypigeon.backend.infrastructure.service.database.impl.jdbc.JdbcClientSupport;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.AuthAccountMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.AuthRefreshSessionMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.ChannelMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.ChannelMemberMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.MessageMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.UserProfileMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service.MybatisPlusAuthAccountDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service.MybatisPlusAuthRefreshSessionDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service.MybatisPlusChannelDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service.MybatisPlusChannelMemberDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service.MybatisPlusMessageDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service.MybatisPlusUserProfileDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.startup.DatabaseInitializationCheck;
import team.carrypigeon.backend.infrastructure.service.database.impl.transaction.SpringTransactionRunner;

/**
 * 数据库服务自动配置。
 * 职责：在 database-impl 内装配数据库服务实现与最小健康检查能力。
 * 边界：只创建具体实现 Bean，不向 database-api 写入任何 Spring 装配逻辑。
 */
@AutoConfiguration(afterName = "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration")
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
     * 创建数据库初始化检查。
     *
     * @param databaseHealthService 数据库健康检查服务
     * @return 共享初始化检查契约下的数据库检查
     */
    @Bean
    @ConditionalOnMissingBean(name = "databaseInitializationCheck")
    public InitializationCheck databaseInitializationCheck(DatabaseHealthService databaseHealthService) {
        return new DatabaseInitializationCheck(databaseHealthService);
    }

    /**
     * 创建鉴权账户数据库服务。
     *
     * @param authAccountMapper 鉴权账户 Mapper
     * @return 鉴权账户数据库服务
     */
    @Bean
    @ConditionalOnMissingBean
    public AuthAccountDatabaseService authAccountDatabaseService(AuthAccountMapper authAccountMapper) {
        return new MybatisPlusAuthAccountDatabaseService(authAccountMapper);
    }

    /**
     * 创建刷新会话数据库服务。
     *
     * @param authRefreshSessionMapper 刷新会话 Mapper
     * @return 刷新会话数据库服务
     */
    @Bean
    @ConditionalOnMissingBean
    public AuthRefreshSessionDatabaseService authRefreshSessionDatabaseService(
            AuthRefreshSessionMapper authRefreshSessionMapper
    ) {
        return new MybatisPlusAuthRefreshSessionDatabaseService(authRefreshSessionMapper);
    }

    /**
     * 创建用户资料数据库服务。
     *
     * @param userProfileMapper 用户资料 Mapper
     * @return 用户资料数据库服务
     */
    @Bean
    @ConditionalOnMissingBean
    public UserProfileDatabaseService userProfileDatabaseService(UserProfileMapper userProfileMapper) {
        return new MybatisPlusUserProfileDatabaseService(userProfileMapper);
    }

    /**
     * 创建频道数据库服务。
     *
     * @param channelMapper 频道 Mapper
     * @return 频道数据库服务
     */
    @Bean
    @ConditionalOnMissingBean
    public ChannelDatabaseService channelDatabaseService(ChannelMapper channelMapper) {
        return new MybatisPlusChannelDatabaseService(channelMapper);
    }

    /**
     * 创建频道成员数据库服务。
     *
     * @param channelMemberMapper 频道成员 Mapper
     * @return 频道成员数据库服务
     */
    @Bean
    @ConditionalOnMissingBean
    public ChannelMemberDatabaseService channelMemberDatabaseService(ChannelMemberMapper channelMemberMapper) {
        return new MybatisPlusChannelMemberDatabaseService(channelMemberMapper);
    }

    /**
     * 创建消息数据库服务。
     *
     * @param messageMapper 消息 Mapper
     * @return 消息数据库服务
     */
    @Bean
    @ConditionalOnMissingBean
    public MessageDatabaseService messageDatabaseService(MessageMapper messageMapper) {
        return new MybatisPlusMessageDatabaseService(messageMapper);
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
