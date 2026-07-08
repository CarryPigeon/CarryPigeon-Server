package team.carrypigeon.backend.chat.domain.features.server.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.message.domain.port.MessageRealtimePublisher;
import team.carrypigeon.backend.chat.domain.features.message.support.payload.MessageAttachmentPayloadResolver;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.NettyMessageRealtimePublisher;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeNotificationPreferenceFilter;
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
                RealtimeNotificationPreferenceFilter.allowAll()
        );

        assertThat(publisher).isInstanceOf(NettyMessageRealtimePublisher.class);
    }
}
