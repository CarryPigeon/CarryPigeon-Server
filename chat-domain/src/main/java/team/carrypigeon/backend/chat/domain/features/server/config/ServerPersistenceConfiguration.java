package team.carrypigeon.backend.chat.domain.features.server.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import team.carrypigeon.backend.chat.domain.features.server.domain.repository.NotificationPreferenceRepository;
import team.carrypigeon.backend.chat.domain.features.server.support.persistence.DatabaseBackedNotificationPreferenceRepository;
import team.carrypigeon.backend.infrastructure.service.database.api.service.NotificationPreferenceDatabaseService;

/**
 * 服务端 feature 持久化装配配置。
 */
@Configuration
@ConditionalOnProperty(prefix = "cp.infrastructure.service.database", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ServerPersistenceConfiguration {

    /**
     * 创建通知偏好仓储适配器。
     *
     * @param notificationPreferenceDatabaseService 通知偏好数据库服务契约
     * @return 面向领域的通知偏好仓储实现
     */
    @Bean
    public NotificationPreferenceRepository notificationPreferenceRepository(NotificationPreferenceDatabaseService notificationPreferenceDatabaseService) {
        return new DatabaseBackedNotificationPreferenceRepository(notificationPreferenceDatabaseService);
    }
}
