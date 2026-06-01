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
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelReadStateDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelPinDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.ChannelAuditLogMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.ChannelBanMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.ChannelInviteMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.ChannelMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.ChannelMemberMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.ChannelReadStateMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.ChannelPinMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service.MybatisPlusChannelAuditLogDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service.MybatisPlusChannelBanDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service.MybatisPlusChannelDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service.MybatisPlusChannelInviteDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service.MybatisPlusChannelMemberDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service.MybatisPlusChannelReadStateDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service.MybatisPlusChannelPinDatabaseService;

/**
 * 频道数据库服务自动配置。
 * 职责：装配 channel feature 所需的数据库服务实现。
 * 边界：不装配 auth / user / message 数据库服务。
 */
@AutoConfiguration(afterName = "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration")
@ConditionalOnProperty(prefix = "cp.infrastructure.service.database", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseChannelServiceAutoConfiguration {

    /**
     * 装配频道基础数据库服务。
     * 输入：频道主表 Mapper。
     * 输出：面向 channel feature 的频道持久化实现。
     *
     * @param channelMapper 频道主表 Mapper
     * @return 频道数据库服务实现
     */
    @Bean
    @ConditionalOnMissingBean
    public ChannelDatabaseService channelDatabaseService(ChannelMapper channelMapper) {
        return new MybatisPlusChannelDatabaseService(channelMapper);
    }

    /**
     * 装配频道成员数据库服务。
     * 输入：频道成员表 Mapper。
     * 输出：供成员加入、退出与角色读取使用的持久化实现。
     *
     * @param channelMemberMapper 频道成员表 Mapper
     * @return 频道成员数据库服务实现
     */
    @Bean
    @ConditionalOnMissingBean
    public ChannelMemberDatabaseService channelMemberDatabaseService(ChannelMemberMapper channelMemberMapper) {
        return new MybatisPlusChannelMemberDatabaseService(channelMemberMapper);
    }

    /**
     * 装配频道邀请数据库服务。
     * 输入：频道邀请表 Mapper。
     * 输出：供邀请创建、校验和撤销使用的持久化实现。
     *
     * @param channelInviteMapper 频道邀请表 Mapper
     * @return 频道邀请数据库服务实现
     */
    @Bean
    @ConditionalOnMissingBean
    public ChannelInviteDatabaseService channelInviteDatabaseService(ChannelInviteMapper channelInviteMapper) {
        return new MybatisPlusChannelInviteDatabaseService(channelInviteMapper);
    }

    /**
     * 装配频道封禁数据库服务。
     * 输入：频道封禁表 Mapper。
     * 输出：供频道封禁规则读写使用的持久化实现。
     *
     * @param channelBanMapper 频道封禁表 Mapper
     * @return 频道封禁数据库服务实现
     */
    @Bean
    @ConditionalOnMissingBean
    public ChannelBanDatabaseService channelBanDatabaseService(ChannelBanMapper channelBanMapper) {
        return new MybatisPlusChannelBanDatabaseService(channelBanMapper);
    }

    /**
     * 装配频道审计日志数据库服务。
     * 输入：频道审计日志表 Mapper。
     * 输出：供审计事件落库与查询使用的持久化实现。
     *
     * @param channelAuditLogMapper 频道审计日志表 Mapper
     * @return 频道审计日志数据库服务实现
     */
    @Bean
    @ConditionalOnMissingBean
    public ChannelAuditLogDatabaseService channelAuditLogDatabaseService(ChannelAuditLogMapper channelAuditLogMapper) {
        return new MybatisPlusChannelAuditLogDatabaseService(channelAuditLogMapper);
    }

    /**
     * 装配频道已读状态数据库服务。
     * 输入：频道已读状态表 Mapper。
     * 输出：供未读计数与阅读进度更新使用的持久化实现。
     *
     * @param channelReadStateMapper 频道已读状态表 Mapper
     * @return 频道已读状态数据库服务实现
     */
    @Bean
    @ConditionalOnMissingBean
    public ChannelReadStateDatabaseService channelReadStateDatabaseService(ChannelReadStateMapper channelReadStateMapper) {
        return new MybatisPlusChannelReadStateDatabaseService(channelReadStateMapper);
    }

    /**
     * 装配频道置顶消息数据库服务。
     * 输入：频道置顶表 Mapper。
     * 输出：供频道置顶消息维护使用的持久化实现。
     *
     * @param channelPinMapper 频道置顶表 Mapper
     * @return 频道置顶数据库服务实现
     */
    @Bean
    @ConditionalOnMissingBean
    public ChannelPinDatabaseService channelPinDatabaseService(ChannelPinMapper channelPinMapper) {
        return new MybatisPlusChannelPinDatabaseService(channelPinMapper);
    }
}
