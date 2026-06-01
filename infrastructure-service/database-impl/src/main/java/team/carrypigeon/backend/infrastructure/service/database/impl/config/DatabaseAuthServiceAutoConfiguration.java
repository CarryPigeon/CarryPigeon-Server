package team.carrypigeon.backend.infrastructure.service.database.impl.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import team.carrypigeon.backend.infrastructure.service.database.api.service.AuthAccountDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.api.service.AuthRefreshSessionDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.api.service.UserProfileDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.AuthAccountMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.AuthRefreshSessionMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.UserProfileMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service.MybatisPlusAuthAccountDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service.MybatisPlusAuthRefreshSessionDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service.MybatisPlusUserProfileDatabaseService;

/**
 * 鉴权与用户数据库服务自动配置。
 * 职责：装配 auth / user feature 所需的数据库服务实现。
 * 边界：不装配 channel 或 message 相关数据库服务。
 */
@AutoConfiguration(afterName = "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration")
@ConditionalOnProperty(prefix = "cp.infrastructure.service.database", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseAuthServiceAutoConfiguration {

    /**
     * 装配鉴权账号数据库服务。
     * 输入：鉴权账号表 Mapper。
     * 输出：供 auth feature 使用的账号持久化实现。
     *
     * @param authAccountMapper 鉴权账号表 Mapper
     * @return 鉴权账号数据库服务实现
     */
    @Bean
    @ConditionalOnMissingBean
    public AuthAccountDatabaseService authAccountDatabaseService(AuthAccountMapper authAccountMapper) {
        return new MybatisPlusAuthAccountDatabaseService(authAccountMapper);
    }

    /**
     * 装配刷新会话数据库服务。
     * 输入：刷新会话表 Mapper。
     * 输出：供 auth feature 使用的 refresh session 持久化实现。
     *
     * @param authRefreshSessionMapper 刷新会话表 Mapper
     * @return 刷新会话数据库服务实现
     */
    @Bean
    @ConditionalOnMissingBean
    public AuthRefreshSessionDatabaseService authRefreshSessionDatabaseService(
            AuthRefreshSessionMapper authRefreshSessionMapper
    ) {
        return new MybatisPlusAuthRefreshSessionDatabaseService(authRefreshSessionMapper);
    }

    /**
     * 装配用户资料数据库服务。
     * 输入：用户资料表 Mapper。
     * 输出：供 user feature 使用的资料持久化实现。
     *
     * @param userProfileMapper 用户资料表 Mapper
     * @return 用户资料数据库服务实现
     */
    @Bean
    @ConditionalOnMissingBean
    public UserProfileDatabaseService userProfileDatabaseService(UserProfileMapper userProfileMapper) {
        return new MybatisPlusUserProfileDatabaseService(userProfileMapper);
    }
}
