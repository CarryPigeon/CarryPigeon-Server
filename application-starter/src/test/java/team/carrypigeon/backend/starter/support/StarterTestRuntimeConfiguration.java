package team.carrypigeon.backend.starter.support;

import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthAccount;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthRefreshSession;
import team.carrypigeon.backend.chat.domain.features.auth.domain.repository.AuthAccountRepository;
import team.carrypigeon.backend.chat.domain.features.auth.domain.repository.AuthRefreshSessionRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.DeleteObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.GetObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PresignedUrl;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PresignedUrlCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PutObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.StorageObject;
import team.carrypigeon.backend.infrastructure.service.storage.api.service.ObjectStorageService;

/**
 * Starter 测试运行时配置。
 * 职责：为 application-starter 下的启动烟雾测试和附件链路回归测试提供稳定、可重置的内存替身。
 * 边界：只服务测试，不替代正式仓储或外部服务实现。
 */
@TestConfiguration(proxyBeanMethods = false)
public class StarterTestRuntimeConfiguration {

    /**
     * 创建共享测试状态。
     *
     * @return 共享测试状态
     */
    @Bean
    public StarterTestState starterTestState() {
        return new StarterTestState();
    }

    /**
     * 创建内存事务执行器。
     *
     * @return 无副作用事务执行器
     */
    @Bean
    @Primary
    public TransactionRunner transactionRunner() {
        return new TransactionRunner() {
            @Override
            public <T> T runInTransaction(java.util.function.Supplier<T> action) {
                return action.get();
            }

            @Override
            public void runInTransaction(Runnable action) {
                action.run();
            }
        };
    }

    /**
     * 创建内存鉴权账户仓储。
     */
    @Bean
    @Primary
    public AuthAccountRepository authAccountRepository(StarterTestState state) {
        return new AuthAccountRepository() {
            @Override
            public Optional<AuthAccount> findByUsername(String username) {
                return Optional.ofNullable(state.accountsByUsername.get(username));
            }

            @Override
            public AuthAccount save(AuthAccount account) {
                state.accountsByUsername.put(account.username(), account);
                state.accountsById.put(account.id(), account);
                return account;
            }
        };
    }

    /**
     * 创建内存 refresh session 仓储。
     */
    @Bean
    @Primary
    public AuthRefreshSessionRepository authRefreshSessionRepository(StarterTestState state) {
        return new AuthRefreshSessionRepository() {
            @Override
            public Optional<AuthRefreshSession> findById(long sessionId) {
                return Optional.ofNullable(state.refreshSessionsById.get(sessionId));
            }

            @Override
            public AuthRefreshSession save(AuthRefreshSession session) {
                state.refreshSessionsById.put(session.id(), session);
                return session;
            }

            @Override
            public void revoke(long sessionId) {
                AuthRefreshSession session = state.refreshSessionsById.get(sessionId);
                if (session == null) {
                    return;
                }
                state.refreshSessionsById.put(sessionId, new AuthRefreshSession(
                        session.id(),
                        session.accountId(),
                        session.refreshTokenHash(),
                        session.expiresAt(),
                        true,
                        session.createdAt(),
                        session.updatedAt()
                ));
            }
        };
    }

    /**
     * 创建内存用户资料仓储。
     */
    @Bean
    @Primary
    public UserProfileRepository userProfileRepository(StarterTestState state) {
        return new UserProfileRepository() {
            @Override
            public Optional<UserProfile> findByAccountId(long accountId) {
                return Optional.ofNullable(state.userProfilesByAccountId.get(accountId));
            }

            @Override
            public UserProfile save(UserProfile userProfile) {
                state.userProfilesByAccountId.put(userProfile.accountId(), userProfile);
                return userProfile;
            }

            @Override
            public UserProfile update(UserProfile userProfile) {
                state.userProfilesByAccountId.put(userProfile.accountId(), userProfile);
                return userProfile;
            }
        };
    }

    /**
     * 创建内存频道仓储。
     */
    @Bean
    @Primary
    public ChannelRepository channelRepository(StarterTestState state) {
        return new ChannelRepository() {
            @Override
            public Optional<Channel> findDefaultChannel() {
                return Optional.ofNullable(state.defaultChannel);
            }

            @Override
            public Optional<Channel> findById(long channelId) {
                return Optional.ofNullable(state.channelsById.get(channelId));
            }
        };
    }

    /**
     * 创建内存频道成员仓储。
     */
    @Bean
    @Primary
    public ChannelMemberRepository channelMemberRepository(StarterTestState state) {
        return new ChannelMemberRepository() {
            @Override
            public boolean exists(long channelId, long accountId) {
                return state.channelMemberAccountIds.getOrDefault(channelId, List.of()).contains(accountId);
            }

            @Override
            public void save(ChannelMember channelMember) {
                state.channelMemberAccountIds
                        .computeIfAbsent(channelMember.channelId(), ignored -> new ArrayList<>())
                        .add(channelMember.accountId());
            }

            @Override
            public List<Long> findAccountIdsByChannelId(long channelId) {
                return List.copyOf(state.channelMemberAccountIds.getOrDefault(channelId, List.of()));
            }
        };
    }

    /**
     * 创建内存消息仓储。
     */
    @Bean
    @Primary
    public MessageRepository messageRepository(StarterTestState state) {
        return new MessageRepository() {
            @Override
            public ChannelMessage save(ChannelMessage message) {
                state.messagesByChannelId.computeIfAbsent(message.channelId(), ignored -> new ArrayList<>()).add(message);
                return message;
            }

            @Override
            public List<ChannelMessage> findByChannelIdBefore(long channelId, Long cursorMessageId, int limit) {
                return state.messagesByChannelId.getOrDefault(channelId, List.of()).stream()
                        .filter(message -> cursorMessageId == null || message.messageId() < cursorMessageId)
                        .sorted(Comparator.comparingLong(ChannelMessage::messageId).reversed())
                        .limit(limit)
                        .toList();
            }

            @Override
            public List<ChannelMessage> searchByChannelId(long channelId, String keyword, int limit) {
                String normalizedKeyword = keyword == null ? "" : keyword.trim();
                return state.messagesByChannelId.getOrDefault(channelId, List.of()).stream()
                        .filter(message -> message.searchableText() != null && message.searchableText().contains(normalizedKeyword))
                        .sorted(Comparator.comparingLong(ChannelMessage::messageId).reversed())
                        .limit(limit)
                        .toList();
            }
        };
    }

    /**
     * 创建内存对象存储实现。
     */
    @Bean
    @Primary
    public ObjectStorageService objectStorageService(StarterTestState state) {
        return new ObjectStorageService() {
            @Override
            public StorageObject put(PutObjectCommand command) {
                StorageObject storageObject = StorageObject.metadata(command.objectKey(), command.contentType(), command.size());
                state.storageObjectsByKey.put(command.objectKey(), storageObject);
                return storageObject;
            }

            @Override
            public Optional<StorageObject> get(GetObjectCommand command) {
                return Optional.ofNullable(state.storageObjectsByKey.get(command.objectKey()));
            }

            @Override
            public void delete(DeleteObjectCommand command) {
                state.storageObjectsByKey.remove(command.objectKey());
            }

            @Override
            public PresignedUrl createPresignedUrl(PresignedUrlCommand command) {
                return new PresignedUrl(
                        URI.create("http://test.local/objects/" + command.objectKey()),
                        Instant.parse("2026-04-23T01:00:00Z")
                );
            }
        };
    }

    /**
     * Starter 测试共享状态。
     * 职责：承载可在测试间重置的内存数据。
     */
    public static class StarterTestState {

        private final Map<String, AuthAccount> accountsByUsername = new ConcurrentHashMap<>();
        private final Map<Long, AuthAccount> accountsById = new ConcurrentHashMap<>();
        private final Map<Long, AuthRefreshSession> refreshSessionsById = new ConcurrentHashMap<>();
        private final Map<Long, UserProfile> userProfilesByAccountId = new ConcurrentHashMap<>();
        private final Map<Long, Channel> channelsById = new ConcurrentHashMap<>();
        private final Map<Long, List<Long>> channelMemberAccountIds = new ConcurrentHashMap<>();
        private final Map<Long, List<ChannelMessage>> messagesByChannelId = new ConcurrentHashMap<>();
        private final Map<String, StorageObject> storageObjectsByKey = new ConcurrentHashMap<>();
        private Channel defaultChannel;

        public StarterTestState() {
            reset();
        }

        /**
         * 重置到最小可运行初始状态。
         */
        public void reset() {
            accountsByUsername.clear();
            accountsById.clear();
            refreshSessionsById.clear();
            userProfilesByAccountId.clear();
            channelsById.clear();
            channelMemberAccountIds.clear();
            messagesByChannelId.clear();
            storageObjectsByKey.clear();
            defaultChannel = new Channel(
                    1L,
                    1L,
                    "public",
                    "public",
                    true,
                    Instant.parse("2026-04-23T00:00:00Z"),
                    Instant.parse("2026-04-23T00:00:00Z")
            );
            channelsById.put(defaultChannel.id(), defaultChannel);
        }
    }
}
