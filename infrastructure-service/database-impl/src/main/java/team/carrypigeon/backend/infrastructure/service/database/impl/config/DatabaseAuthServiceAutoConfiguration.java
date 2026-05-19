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

    @Bean
    @ConditionalOnMissingBean
    public AuthAccountDatabaseService authAccountDatabaseService(AuthAccountMapper authAccountMapper) {
        return new MybatisPlusAuthAccountDatabaseService(authAccountMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthRefreshSessionDatabaseService authRefreshSessionDatabaseService(
            AuthRefreshSessionMapper authRefreshSessionMapper
    ) {
        return new MybatisPlusAuthRefreshSessionDatabaseService(authRefreshSessionMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public UserProfileDatabaseService userProfileDatabaseService(UserProfileMapper userProfileMapper) {
        return new MybatisPlusUserProfileDatabaseService(userProfileMapper);
    }
}
