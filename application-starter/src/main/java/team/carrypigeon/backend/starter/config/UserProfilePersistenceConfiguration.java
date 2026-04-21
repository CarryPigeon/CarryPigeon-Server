package team.carrypigeon.backend.starter.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
import team.carrypigeon.backend.infrastructure.service.database.api.service.UserProfileDatabaseService;

/**
 * 用户资料持久化装配配置。
 * 职责：在 application-starter 中装配 user 领域仓储抽象到数据库服务契约之间的运行时适配器。
 * 边界：这里只负责运行时 Bean 装配，不承载用户资料业务规则，也不承载 JDBC 实现细节。
 */
@Configuration
public class UserProfilePersistenceConfiguration {

    /**
     * 创建用户资料仓储适配器。
     *
     * @param userProfileDatabaseService 用户资料数据库服务契约
     * @return 面向 chat-domain 的用户资料仓储实现
     */
    @Bean
    @ConditionalOnBean(UserProfileDatabaseService.class)
    public UserProfileRepository userProfileRepository(UserProfileDatabaseService userProfileDatabaseService) {
        return new StarterUserProfileRepository(userProfileDatabaseService);
    }
}
