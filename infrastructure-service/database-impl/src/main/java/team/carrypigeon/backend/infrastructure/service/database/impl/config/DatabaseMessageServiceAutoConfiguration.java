package team.carrypigeon.backend.infrastructure.service.database.impl.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import team.carrypigeon.backend.infrastructure.service.database.api.service.MessageDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.MessageMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service.MybatisPlusMessageDatabaseService;

/**
 * 消息数据库服务自动配置。
 * 职责：装配 message feature 所需的数据库服务实现。
 * 边界：不装配其它 feature 的数据库服务。
 */
@AutoConfiguration(afterName = "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration")
@ConditionalOnProperty(prefix = "cp.infrastructure.service.database", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseMessageServiceAutoConfiguration {

    /**
     * 装配消息数据库服务。
     * 输入：消息表 Mapper。
     * 输出：供 message feature 使用的持久化实现。
     *
     * @param messageMapper 消息表 Mapper
     * @return 消息数据库服务实现
     */
    @Bean
    @ConditionalOnMissingBean
    public MessageDatabaseService messageDatabaseService(MessageMapper messageMapper) {
        return new MybatisPlusMessageDatabaseService(messageMapper);
    }
}
