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
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelBan;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelInvite;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelPin;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelReadState;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelUnread;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelBanRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelInviteRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelPinRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelReadStateRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.MessageStatus;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.Mention;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MentionRepository;
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
            public Optional<AuthAccount> findById(long accountId) {
                return Optional.ofNullable(state.accountsById.get(accountId));
            }

            @Override
            public AuthAccount save(AuthAccount account) {
                state.accountsByUsername.put(account.username(), account);
                state.accountsById.put(account.id(), account);
                return account;
            }

            @Override
            public AuthAccount update(AuthAccount account) {
                state.accountsById.put(account.id(), account);
                state.accountsByUsername.values().removeIf(existing -> existing.id() == account.id());
                state.accountsByUsername.put(account.username(), account);
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
            public List<UserProfile> findAll() {
                return new ArrayList<>(state.userProfilesByAccountId.values());
            }

            @Override
            public List<UserProfile> findByAccountIdBefore(Long cursorAccountId, int limit) {
                return state.userProfilesByAccountId.values().stream()
                        .filter(userProfile -> cursorAccountId == null || userProfile.accountId() < cursorAccountId)
                        .sorted(Comparator.comparingLong(UserProfile::accountId).reversed())
                        .limit(limit)
                        .toList();
            }

            @Override
            public List<UserProfile> searchByKeyword(String keyword, Long cursorAccountId, int limit) {
                String normalizedKeyword = keyword == null ? "" : keyword.trim();
                return state.userProfilesByAccountId.values().stream()
                        .filter(userProfile -> cursorAccountId == null || userProfile.accountId() < cursorAccountId)
                        .filter(userProfile -> userProfile.nickname().contains(normalizedKeyword) || userProfile.bio().contains(normalizedKeyword))
                        .sorted(Comparator.comparingLong(UserProfile::accountId).reversed())
                        .limit(limit)
                        .toList();
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
            public Optional<Channel> findSystemChannel() {
                return state.channelsById.values().stream()
                        .filter(channel -> "system".equals(channel.type()))
                        .findFirst();
            }

            @Override
            public Optional<Channel> findById(long channelId) {
                return Optional.ofNullable(state.channelsById.get(channelId));
            }

            @Override
            public Channel save(Channel channel) {
                state.channelsById.put(channel.id(), channel);
                return channel;
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
                return state.channelMembersByChannelId.getOrDefault(channelId, List.of()).stream()
                        .anyMatch(member -> member.accountId() == accountId);
            }

            @Override
            public void save(ChannelMember channelMember) {
                state.channelMembersByChannelId
                        .computeIfAbsent(channelMember.channelId(), ignored -> new ArrayList<>())
                        .add(channelMember);
            }

            @Override
            public Optional<ChannelMember> findByChannelIdAndAccountId(long channelId, long accountId) {
                return state.channelMembersByChannelId.getOrDefault(channelId, List.of()).stream()
                        .filter(member -> member.accountId() == accountId)
                        .findFirst();
            }

            @Override
            public List<ChannelMember> findByChannelId(long channelId) {
                return List.copyOf(state.channelMembersByChannelId.getOrDefault(channelId, List.of()));
            }

            @Override
            public List<Long> findAccountIdsByChannelId(long channelId) {
                return state.channelMembersByChannelId.getOrDefault(channelId, List.of()).stream()
                        .map(ChannelMember::accountId)
                        .toList();
            }
        };
    }

    /**
     * 创建内存频道已读状态仓储。
     */
    @Bean
    @Primary
    public ChannelReadStateRepository channelReadStateRepository(StarterTestState state) {
        return new ChannelReadStateRepository() {
            @Override
            public Optional<ChannelReadState> findByChannelIdAndAccountId(long channelId, long accountId) {
                return Optional.ofNullable(state.channelReadStatesByKey.get(key(channelId, accountId)));
            }

            @Override
            public ChannelReadState upsert(ChannelReadState readState) {
                state.channelReadStatesByKey.put(key(readState.channelId(), readState.accountId()), readState);
                return readState;
            }

            @Override
            public List<ChannelUnread> listUnreadsByAccountId(long accountId) {
                return state.channelMembersByChannelId.entrySet().stream()
                        .filter(entry -> entry.getValue().stream().anyMatch(member -> member.accountId() == accountId))
                        .map(entry -> {
                            ChannelReadState readState = state.channelReadStatesByKey.get(key(entry.getKey(), accountId));
                            long lastReadMessageId = readState == null ? 0L : readState.lastReadMessageId();
                            Instant lastReadTime = readState == null ? Instant.parse("1970-01-01T00:00:00Z") : readState.lastReadTime();
                            long unreadCount = state.messagesByChannelId.getOrDefault(entry.getKey(), List.of()).stream()
                                    .filter(message -> message.messageId() > lastReadMessageId)
                                    .filter(message -> message.senderId() != accountId)
                                    .count();
                            return new ChannelUnread(entry.getKey(), unreadCount, lastReadTime);
                        })
                        .filter(unread -> unread.unreadCount() > 0)
                        .toList();
            }

            private String key(long channelId, long accountId) {
                return channelId + ":" + accountId;
            }
        };
    }

    /**
     * 创建内存频道邀请仓储。
     */
    @Bean
    @Primary
    public ChannelInviteRepository channelInviteRepository() {
        return new ChannelInviteRepository() {
            @Override
            public Optional<ChannelInvite> findByChannelIdAndInviteeAccountId(long channelId, long inviteeAccountId) {
                return Optional.empty();
            }

            @Override
            public void save(ChannelInvite channelInvite) {
            }

            @Override
            public void update(ChannelInvite channelInvite) {
            }
        };
    }

    /**
     * 创建内存频道封禁仓储。
     */
    @Bean
    @Primary
    public ChannelBanRepository channelBanRepository() {
        return new ChannelBanRepository() {
            @Override
            public Optional<ChannelBan> findByChannelIdAndBannedAccountId(long channelId, long bannedAccountId) {
                return Optional.empty();
            }

            @Override
            public void save(ChannelBan channelBan) {
            }

            @Override
            public void update(ChannelBan channelBan) {
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
            public Optional<ChannelMessage> findById(long messageId) {
                return state.messagesByChannelId.values().stream()
                        .flatMap(List::stream)
                        .filter(message -> message.messageId() == messageId)
                        .findFirst();
            }

            @Override
            public ChannelMessage update(ChannelMessage message) {
                List<ChannelMessage> messages = state.messagesByChannelId.computeIfAbsent(message.channelId(), ignored -> new ArrayList<>());
                messages.removeIf(existing -> existing.messageId() == message.messageId());
                messages.add(message);
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
                        .filter(message -> message.status() == MessageStatus.SENT)
                        .filter(message -> message.preview().contains(normalizedKeyword)
                                || String.valueOf(message.data()).contains(normalizedKeyword))
                        .sorted(Comparator.comparingLong(ChannelMessage::messageId).reversed())
                        .limit(limit)
                        .toList();
            }
        };
    }

    /**
     * 创建内存频道置顶仓储。
     */
    @Bean
    @Primary
    public ChannelPinRepository channelPinRepository(StarterTestState state) {
        return new ChannelPinRepository() {
            @Override
            public Optional<ChannelPin> findByChannelIdAndMessageId(long channelId, long messageId) {
                return state.channelPins.stream()
                        .filter(channelPin -> channelPin.channelId() == channelId && channelPin.messageId() == messageId)
                        .findFirst();
            }

            @Override
            public void save(ChannelPin channelPin) {
                state.channelPins.removeIf(existing -> existing.channelId() == channelPin.channelId() && existing.messageId() == channelPin.messageId());
                state.channelPins.add(channelPin);
            }

            @Override
            public void delete(long channelId, long messageId) {
                state.channelPins.removeIf(channelPin -> channelPin.channelId() == channelId && channelPin.messageId() == messageId);
            }

            @Override
            public List<ChannelPin> findByChannelIdBefore(long channelId, Long cursorMessageId, int limit) {
                return state.channelPins.stream()
                        .filter(channelPin -> channelPin.channelId() == channelId)
                        .filter(channelPin -> cursorMessageId == null || channelPin.messageId() < cursorMessageId)
                        .sorted(Comparator.comparingLong(ChannelPin::messageId).reversed())
                        .limit(limit)
                        .toList();
            }

            @Override
            public long countByChannelId(long channelId) {
                return state.channelPins.stream()
                        .filter(channelPin -> channelPin.channelId() == channelId)
                        .count();
            }
        };
    }

    /**
     * 创建内存提及仓储。
     */
    @Bean
    @Primary
    public MentionRepository mentionRepository(StarterTestState state) {
        return new MentionRepository() {
            @Override
            public void save(Mention mention) {
                state.mentions.removeIf(existing -> existing.mentionId() == mention.mentionId());
                state.mentions.add(mention);
            }

            @Override
            public void deleteByMessageId(long messageId) {
                state.mentions.removeIf(mention -> mention.messageId() == messageId);
            }

            @Override
            public List<Mention> listByAccountId(long accountId, Long cursorMentionId, int limit, boolean unreadOnly, Long channelId) {
                return state.mentions.stream()
                        .filter(mention -> mention.targetAccountId() == accountId)
                        .filter(mention -> cursorMentionId == null || mention.mentionId() < cursorMentionId)
                        .filter(mention -> !unreadOnly || !mention.read())
                        .filter(mention -> channelId == null || mention.channelId() == channelId)
                        .sorted(Comparator.comparingLong(Mention::mentionId).reversed())
                        .limit(limit)
                        .toList();
            }

            @Override
            public boolean markAsRead(long accountId, long mentionId) {
                for (int index = 0; index < state.mentions.size(); index++) {
                    Mention mention = state.mentions.get(index);
                    if (mention.targetAccountId() == accountId && mention.mentionId() == mentionId) {
                        state.mentions.set(index, new Mention(
                                mention.mentionId(),
                                mention.channelId(),
                                mention.messageId(),
                                mention.fromAccountId(),
                                mention.targetType(),
                                mention.targetAccountId(),
                                mention.createdAt(),
                                true
                        ));
                        return true;
                    }
                }
                return false;
            }

            @Override
            public int markAllAsRead(long accountId, Long beforeMentionId, Long channelId) {
                int updatedCount = 0;
                for (int index = 0; index < state.mentions.size(); index++) {
                    Mention mention = state.mentions.get(index);
                    boolean matches = mention.targetAccountId() == accountId
                            && !mention.read()
                            && (beforeMentionId == null || mention.mentionId() <= beforeMentionId)
                            && (channelId == null || mention.channelId() == channelId);
                    if (!matches) {
                        continue;
                    }
                    state.mentions.set(index, new Mention(
                            mention.mentionId(),
                            mention.channelId(),
                            mention.messageId(),
                            mention.fromAccountId(),
                            mention.targetType(),
                            mention.targetAccountId(),
                            mention.createdAt(),
                            true
                    ));
                    updatedCount += 1;
                }
                return updatedCount;
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
        private final Map<Long, List<ChannelMember>> channelMembersByChannelId = new ConcurrentHashMap<>();
        private final Map<String, ChannelReadState> channelReadStatesByKey = new ConcurrentHashMap<>();
        private final Map<Long, List<ChannelMessage>> messagesByChannelId = new ConcurrentHashMap<>();
        private final Map<String, StorageObject> storageObjectsByKey = new ConcurrentHashMap<>();
        private final List<ChannelPin> channelPins = new ArrayList<>();
        private final List<Mention> mentions = new ArrayList<>();
        private Channel defaultChannel;
        private Channel systemChannel;

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
            channelMembersByChannelId.clear();
            channelReadStatesByKey.clear();
            messagesByChannelId.clear();
            storageObjectsByKey.clear();
            channelPins.clear();
            mentions.clear();
            defaultChannel = new Channel(
                    1L,
                    1L,
                    "public",
                    "",
                    "",
                    "",
                    "public",
                    true,
                    Instant.parse("2026-04-23T00:00:00Z"),
                    Instant.parse("2026-04-23T00:00:00Z")
            );
            systemChannel = new Channel(
                    2L,
                    1L,
                    "system",
                    "",
                    "",
                    "",
                    "system",
                    false,
                    Instant.parse("2026-04-23T00:00:00Z"),
                    Instant.parse("2026-04-23T00:00:00Z")
            );
            channelsById.put(defaultChannel.id(), defaultChannel);
            channelsById.put(systemChannel.id(), systemChannel);
        }
    }
}
