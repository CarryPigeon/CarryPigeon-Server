package team.carrypigeon.backend.chat.domain.features.channel.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelAuditLogRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelBanRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelInviteRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.features.channel.support.persistence.DatabaseBackedChannelAuditLogRepository;
import team.carrypigeon.backend.chat.domain.features.channel.support.persistence.DatabaseBackedChannelBanRepository;
import team.carrypigeon.backend.chat.domain.features.channel.support.persistence.DatabaseBackedChannelInviteRepository;
import team.carrypigeon.backend.chat.domain.features.channel.support.persistence.DatabaseBackedChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.support.persistence.DatabaseBackedChannelRepository;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelAuditLogDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelBanDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelInviteDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelMemberDatabaseService;

/**
 * 频道持久化装配配置。
 * 职责：在 channel feature 内装配领域仓储与 database-api 契约之间的适配器。
 * 边界：这里只负责 Bean 装配，不承载频道业务规则与 JDBC 细节。
 */
@Configuration
@ConditionalOnProperty(prefix = "cp.infrastructure.service.database", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ChannelPersistenceConfiguration {

    /**
     * 创建频道仓储适配器。
     *
     * @param channelDatabaseService 频道数据库服务契约
     * @return 面向领域的频道仓储实现
     */
    @Bean
    public ChannelRepository channelRepository(ChannelDatabaseService channelDatabaseService) {
        return new DatabaseBackedChannelRepository(channelDatabaseService);
    }

    /**
     * 创建频道成员仓储适配器。
     *
     * @param channelMemberDatabaseService 频道成员数据库服务契约
     * @return 面向领域的频道成员仓储实现
     */
    @Bean
    public ChannelMemberRepository channelMemberRepository(ChannelMemberDatabaseService channelMemberDatabaseService) {
        return new DatabaseBackedChannelMemberRepository(channelMemberDatabaseService);
    }

    /**
     * 创建频道邀请仓储适配器。
     *
     * @param channelInviteDatabaseService 频道邀请数据库服务契约
     * @return 面向领域的频道邀请仓储实现
     */
    @Bean
    public ChannelInviteRepository channelInviteRepository(ChannelInviteDatabaseService channelInviteDatabaseService) {
        return new DatabaseBackedChannelInviteRepository(channelInviteDatabaseService);
    }

    /**
     * 创建频道封禁仓储适配器。
     *
     * @param channelBanDatabaseService 频道封禁数据库服务契约
     * @return 面向领域的频道封禁仓储实现
     */
    @Bean
    public ChannelBanRepository channelBanRepository(ChannelBanDatabaseService channelBanDatabaseService) {
        return new DatabaseBackedChannelBanRepository(channelBanDatabaseService);
    }

    /**
     * 创建频道审计日志仓储适配器。
     *
     * @param channelAuditLogDatabaseService 频道审计日志数据库服务契约
     * @return 面向领域的频道审计日志仓储实现
     */
    @Bean
    public ChannelAuditLogRepository channelAuditLogRepository(ChannelAuditLogDatabaseService channelAuditLogDatabaseService) {
        return new DatabaseBackedChannelAuditLogRepository(channelAuditLogDatabaseService);
    }
}
