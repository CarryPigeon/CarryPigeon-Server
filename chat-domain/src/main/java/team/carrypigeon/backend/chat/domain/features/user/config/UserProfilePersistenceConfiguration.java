package team.carrypigeon.backend.chat.domain.features.user.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
import team.carrypigeon.backend.chat.domain.features.user.support.persistence.DatabaseBackedUserProfileRepository;
import team.carrypigeon.backend.infrastructure.service.database.api.service.UserProfileDatabaseService;

/**
 * 用户资料持久化装配配置。
 * 职责：在 user feature 内装配领域仓储抽象到 database-api 服务契约之间的运行时适配器。
 * 边界：这里只负责 Bean 装配，不承载用户资料业务规则，也不承载 JDBC 实现细节。
 */
@Configuration
@ConditionalOnProperty(prefix = "cp.infrastructure.service.database", name = "enabled", havingValue = "true", matchIfMissing = true)
public class UserProfilePersistenceConfiguration {

    /**
     * 创建用户资料仓储适配器。
     *
     * @param userProfileDatabaseService 用户资料数据库服务契约
     * @return 面向 user 领域的用户资料仓储实现
     */
    @Bean
    public UserProfileRepository userProfileRepository(UserProfileDatabaseService userProfileDatabaseService) {
        return new DatabaseBackedUserProfileRepository(userProfileDatabaseService);
    }
}
