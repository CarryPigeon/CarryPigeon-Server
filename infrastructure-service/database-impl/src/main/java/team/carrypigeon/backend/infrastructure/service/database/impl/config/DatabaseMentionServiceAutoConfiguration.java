package team.carrypigeon.backend.infrastructure.service.database.impl.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import team.carrypigeon.backend.infrastructure.service.database.api.service.MentionDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.MentionMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service.MybatisPlusMentionDatabaseService;

/**
 * 提及数据库服务自动配置。
 * 职责：装配 mention inbox 所需的数据库服务实现。
 * 边界：不装配其它 feature 的数据库服务。
 */
@AutoConfiguration(afterName = "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration")
@ConditionalOnProperty(prefix = "cp.infrastructure.service.database", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseMentionServiceAutoConfiguration {

    /**
     * 装配提及数据库服务。
     * 输入：提及表 Mapper。
     * 输出：供 mention feature 使用的持久化实现。
     *
     * @param mentionMapper 提及表 Mapper
     * @return 提及数据库服务实现
     */
    @Bean
    @ConditionalOnMissingBean
    public MentionDatabaseService mentionDatabaseService(MentionMapper mentionMapper) {
        return new MybatisPlusMentionDatabaseService(mentionMapper);
    }
}
