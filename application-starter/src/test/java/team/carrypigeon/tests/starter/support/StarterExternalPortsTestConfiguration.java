package team.carrypigeon.tests.starter.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import team.carrypigeon.backend.chat.domain.features.auth.domain.repository.AuthAccountRepository;
import team.carrypigeon.backend.chat.domain.features.auth.domain.repository.AuthRefreshSessionRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelAuditLogRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelBanRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelInviteRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelPinRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.ChannelReadStateRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MentionRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageIdempotencyRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.chat.domain.features.server.domain.repository.NotificationPreferenceRepository;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

import static org.mockito.Mockito.mock;

/**
 * ApplicationStarter smoke 测试的外部端口替身。
 * 职责：在关闭真实外部服务实现时提供领域装配所需的仓储与事务边界。
 * 边界：只声明端口替身，不手工创建领域服务、控制器或其它生产对象。
 */
@TestConfiguration(proxyBeanMethods = false)
public class StarterExternalPortsTestConfiguration {

    @Bean
    public AuthAccountRepository authAccountRepository() {
        return mock(AuthAccountRepository.class);
    }

    @Bean
    public AuthRefreshSessionRepository authRefreshSessionRepository() {
        return mock(AuthRefreshSessionRepository.class);
    }

    @Bean
    public UserProfileRepository userProfileRepository() {
        return mock(UserProfileRepository.class);
    }

    @Bean
    public ChannelRepository channelRepository() {
        return mock(ChannelRepository.class);
    }

    @Bean
    public ChannelMemberRepository channelMemberRepository() {
        return mock(ChannelMemberRepository.class);
    }

    @Bean
    public ChannelInviteRepository channelInviteRepository() {
        return mock(ChannelInviteRepository.class);
    }

    @Bean
    public ChannelBanRepository channelBanRepository() {
        return mock(ChannelBanRepository.class);
    }

    @Bean
    public ChannelAuditLogRepository channelAuditLogRepository() {
        return mock(ChannelAuditLogRepository.class);
    }

    @Bean
    public ChannelPinRepository channelPinRepository() {
        return mock(ChannelPinRepository.class);
    }

    @Bean
    public MessageRepository messageRepository() {
        return mock(MessageRepository.class);
    }

    @Bean
    public MessageIdempotencyRepository messageIdempotencyRepository() {
        return mock(MessageIdempotencyRepository.class);
    }

    @Bean
    public MentionRepository mentionRepository() {
        return mock(MentionRepository.class);
    }

    @Bean
    public ChannelReadStateRepository channelReadStateRepository() {
        return mock(ChannelReadStateRepository.class);
    }

    @Bean
    public NotificationPreferenceRepository notificationPreferenceRepository() {
        return mock(NotificationPreferenceRepository.class);
    }

    @Bean
    public TransactionRunner transactionRunner() {
        return mock(TransactionRunner.class);
    }
}
