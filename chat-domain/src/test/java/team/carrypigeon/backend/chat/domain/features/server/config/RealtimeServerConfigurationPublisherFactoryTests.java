package team.carrypigeon.backend.chat.domain.features.server.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.MessageRealtimePublisher;
import team.carrypigeon.backend.chat.domain.features.message.support.payload.MessageAttachmentPayloadResolver;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.NettyMessageRealtimePublisher;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RealtimeServerConfiguration 发布器工厂测试。
 * 职责：验证 realtime 发布器工厂能创建新的 Netty 事件发布器实例。
 * 边界：不验证 Spring 上下文装配，只验证工厂方法本身的构造契约。
 */
@Tag("unit")
class RealtimeServerConfigurationPublisherFactoryTests {

    /**
     * 验证 realtime 发布器工厂会返回 Netty 实现。
     */
    @Test
    @DisplayName("message realtime publisher creates netty publisher instance")
    void messageRealtimePublisher_createsNettyPublisherInstance() {
        RealtimeServerConfiguration configuration = new RealtimeServerConfiguration();
        MessageAttachmentPayloadResolver resolver = new MessageAttachmentPayloadResolver(null, null);

        MessageRealtimePublisher publisher = configuration.messageRealtimePublisher(
                new team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeSessionRegistry(),
                null,
                null,
                new IdGenerator() {
                    @Override
                    public long nextLongId() {
                        return 1L;
                    }
                },
                resolver,
                new UserProfileRepository() {
                    @Override
                    public java.util.Optional<team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile> findByAccountId(long accountId) {
                        return java.util.Optional.empty();
                    }

                    @Override
                    public java.util.List<team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile> findAll() {
                        return java.util.List.of();
                    }

                    @Override
                    public java.util.List<team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile> findByAccountIdBefore(Long cursorAccountId, int limit) {
                        return java.util.List.of();
                    }

                    @Override
                    public java.util.List<team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile> searchByKeyword(String keyword, Long cursorAccountId, int limit) {
                        return java.util.List.of();
                    }

                    @Override
                    public team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile save(team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile userProfile) {
                        return userProfile;
                    }

                    @Override
                    public team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile update(team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile userProfile) {
                        return userProfile;
                    }
                }
        );

        assertThat(publisher).isInstanceOf(NettyMessageRealtimePublisher.class);
    }
}
