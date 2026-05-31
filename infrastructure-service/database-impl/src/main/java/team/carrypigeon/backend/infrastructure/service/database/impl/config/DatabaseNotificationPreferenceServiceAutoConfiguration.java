package team.carrypigeon.backend.infrastructure.service.database.impl.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import team.carrypigeon.backend.infrastructure.service.database.api.service.NotificationPreferenceDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.NotificationPreferenceMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service.MybatisPlusNotificationPreferenceDatabaseService;

/**
 * 通知偏好数据库服务自动配置。
 */
@AutoConfiguration(afterName = "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration")
@ConditionalOnProperty(prefix = "cp.infrastructure.service.database", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseNotificationPreferenceServiceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public NotificationPreferenceDatabaseService notificationPreferenceDatabaseService(NotificationPreferenceMapper notificationPreferenceMapper) {
        return new MybatisPlusNotificationPreferenceDatabaseService(notificationPreferenceMapper);
    }
}
