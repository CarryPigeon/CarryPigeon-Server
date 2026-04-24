package team.carrypigeon.backend.chat.domain.features.server.config;

import java.lang.reflect.Field;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.MessageRealtimePublisher;
import team.carrypigeon.backend.chat.domain.features.message.support.payload.MessageAttachmentPayloadResolver;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.NettyMessageRealtimePublisher;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RealtimeServerConfiguration 发布器工厂测试。
 * 职责：验证 realtime 发布器工厂会复用传入的附件 payload resolver。
 * 边界：不验证 Spring 上下文装配，只验证工厂方法本身的构造契约。
 */
@Tag("unit")
class RealtimeServerConfigurationPublisherFactoryTests {

    /**
     * 验证 realtime 发布器会复用已装配的附件 payload resolver Bean。
     */
    @Test
    @DisplayName("message realtime publisher reuses shared attachment payload resolver bean")
    void messageRealtimePublisher_reusesSharedAttachmentPayloadResolverBean() throws Exception {
        RealtimeServerConfiguration configuration = new RealtimeServerConfiguration();
        MessageAttachmentPayloadResolver resolver = new MessageAttachmentPayloadResolver(null, null);

        MessageRealtimePublisher publisher = configuration.messageRealtimePublisher(
                new team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeSessionRegistry(),
                null,
                null,
                resolver
        );

        assertThat(publisher).isInstanceOf(NettyMessageRealtimePublisher.class);
        Field field = NettyMessageRealtimePublisher.class.getDeclaredField("messageAttachmentPayloadResolver");
        field.setAccessible(true);
        assertThat(field.get(publisher)).isSameAs(resolver);
    }
}
