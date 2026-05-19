package team.carrypigeon.backend.infrastructure.service.database.impl.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelAuditLogDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelBanDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelInviteDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelMemberDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.ChannelAuditLogMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.ChannelBanMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.ChannelInviteMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.ChannelMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.ChannelMemberMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service.MybatisPlusChannelAuditLogDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service.MybatisPlusChannelBanDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service.MybatisPlusChannelDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service.MybatisPlusChannelInviteDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service.MybatisPlusChannelMemberDatabaseService;

/**
 * 频道数据库服务自动配置。
 * 职责：装配 channel feature 所需的数据库服务实现。
 * 边界：不装配 auth / user / message 数据库服务。
 */
@AutoConfiguration(afterName = "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration")
@ConditionalOnProperty(prefix = "cp.infrastructure.service.database", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseChannelServiceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ChannelDatabaseService channelDatabaseService(ChannelMapper channelMapper) {
        return new MybatisPlusChannelDatabaseService(channelMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public ChannelMemberDatabaseService channelMemberDatabaseService(ChannelMemberMapper channelMemberMapper) {
        return new MybatisPlusChannelMemberDatabaseService(channelMemberMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public ChannelInviteDatabaseService channelInviteDatabaseService(ChannelInviteMapper channelInviteMapper) {
        return new MybatisPlusChannelInviteDatabaseService(channelInviteMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public ChannelBanDatabaseService channelBanDatabaseService(ChannelBanMapper channelBanMapper) {
        return new MybatisPlusChannelBanDatabaseService(channelBanMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public ChannelAuditLogDatabaseService channelAuditLogDatabaseService(ChannelAuditLogMapper channelAuditLogMapper) {
        return new MybatisPlusChannelAuditLogDatabaseService(channelAuditLogMapper);
    }
}
