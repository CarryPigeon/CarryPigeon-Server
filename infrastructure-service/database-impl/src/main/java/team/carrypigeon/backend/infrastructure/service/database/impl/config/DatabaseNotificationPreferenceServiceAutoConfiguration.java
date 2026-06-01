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
 * 职责：装配 server feature 所需的通知偏好数据库服务实现。
 * 边界：不装配其它 feature 的数据库服务。
 */
@AutoConfiguration(afterName = "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration")
@ConditionalOnProperty(prefix = "cp.infrastructure.service.database", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseNotificationPreferenceServiceAutoConfiguration {

    /**
     * 装配通知偏好数据库服务。
     * 输入：通知偏好表 Mapper。
     * 输出：供 server feature 使用的持久化实现。
     *
     * @param notificationPreferenceMapper 通知偏好表 Mapper
     * @return 通知偏好数据库服务实现
     */
    @Bean
    @ConditionalOnMissingBean
    public NotificationPreferenceDatabaseService notificationPreferenceDatabaseService(NotificationPreferenceMapper notificationPreferenceMapper) {
        return new MybatisPlusNotificationPreferenceDatabaseService(notificationPreferenceMapper);
    }
}
