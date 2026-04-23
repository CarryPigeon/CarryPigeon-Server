package team.carrypigeon.backend.chat.domain.features.server.support.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.support.payload.MessageAttachmentPayloadResolver;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.DeleteObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.GetObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PresignedUrl;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PresignedUrlCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PutObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.StorageObject;
import team.carrypigeon.backend.infrastructure.service.storage.api.service.ObjectStorageService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * NettyMessageRealtimePublisher 契约测试。
 * 职责：验证 realtime 下发时 file / voice 消息会派生可访问附件字段。
 * 边界：不验证完整 Netty 握手与鉴权链，只验证发布器出站载荷语义。
 */
class NettyMessageRealtimePublisherTests {

    /**
     * 验证 file 消息 realtime 下发会附加临时访问 URL。
     */
    @Test
    @DisplayName("publish file message adds access url to realtime payload")
    void publish_fileMessage_addsAccessUrlToRealtimePayload() {
        JsonProvider jsonProvider = jsonProvider();
        TestObjectStorageService storageService = new TestObjectStorageService();
        storageService.presignedUrl = new PresignedUrl(
                URI.create("http://127.0.0.1:9000/file/demo.pdf?token=abc"),
                Instant.parse("2026-04-22T00:30:00Z")
        );
        RealtimeSessionRegistry realtimeSessionRegistry = new RealtimeSessionRegistry();
        EmbeddedChannel channel = new EmbeddedChannel();
        realtimeSessionRegistry.register(1001L, channel);
        NettyMessageRealtimePublisher publisher = new NettyMessageRealtimePublisher(
                realtimeSessionRegistry,
                jsonProvider,
                new TimeProvider(Clock.fixed(Instant.parse("2026-04-22T00:00:00Z"), ZoneOffset.UTC)),
                new MessageAttachmentPayloadResolver(objectProvider(storageService), jsonProvider)
        );

        publisher.publish(new ChannelMessage(
                5001L,
                "carrypigeon-local",
                1L,
                1L,
                1002L,
                "file",
                "项目文档",
                "[文件消息] demo.pdf",
                "项目文档 demo.pdf",
                jsonProvider.toJson(Map.of(
                        "object_key", "channels/1/messages/file/accounts/1001/5001-demo.pdf",
                        "filename", "demo.pdf",
                        "mime_type", "application/pdf",
                        "size", 123L
                )),
                null,
                "sent",
                Instant.parse("2026-04-22T00:00:00Z")
        ), List.of(1001L));

        TextWebSocketFrame frame = channel.readOutbound();

        assertNotNull(frame);
        String payload = jsonProvider.readTree(frame.text()).path("data").path("payload").asText();
        assertEquals("http://127.0.0.1:9000/file/demo.pdf?token=abc", jsonProvider.readTree(payload).path("access_url").asText());
        assertEquals("channels/1/messages/file/accounts/1001/5001-demo.pdf", jsonProvider.readTree(payload).path("object_key").asText());
    }

    /**
     * 验证 text 消息 realtime 下发不会错误附加附件访问字段。
     */
    @Test
    @DisplayName("publish text message keeps realtime payload unchanged")
    void publish_textMessage_keepsRealtimePayloadUnchanged() {
        JsonProvider jsonProvider = jsonProvider();
        RealtimeSessionRegistry realtimeSessionRegistry = new RealtimeSessionRegistry();
        EmbeddedChannel channel = new EmbeddedChannel();
        realtimeSessionRegistry.register(1001L, channel);
        NettyMessageRealtimePublisher publisher = new NettyMessageRealtimePublisher(
                realtimeSessionRegistry,
                jsonProvider,
                new TimeProvider(Clock.fixed(Instant.parse("2026-04-22T00:00:00Z"), ZoneOffset.UTC)),
                new MessageAttachmentPayloadResolver(objectProvider(null), jsonProvider)
        );

        publisher.publish(new ChannelMessage(
                5002L,
                "carrypigeon-local",
                1L,
                1L,
                1001L,
                "text",
                "hello",
                "hello",
                "hello",
                null,
                null,
                "sent",
                Instant.parse("2026-04-22T00:00:00Z")
        ), List.of(1001L));

        TextWebSocketFrame frame = channel.readOutbound();

        assertNotNull(frame);
        String payload = jsonProvider.readTree(frame.text()).path("data").path("payload").asText();
        assertFalse(jsonProvider.readTree(payload).has("access_url"));
    }

    private static JsonProvider jsonProvider() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return new JsonProvider(objectMapper);
    }

    private static ObjectProvider<ObjectStorageService> objectProvider(ObjectStorageService storageService) {
        return new ObjectProvider<>() {
            @Override
            public ObjectStorageService getObject(Object... args) {
                return storageService;
            }

            @Override
            public ObjectStorageService getIfAvailable() {
                return storageService;
            }

            @Override
            public ObjectStorageService getIfUnique() {
                return storageService;
            }

            @Override
            public ObjectStorageService getObject() {
                return storageService;
            }
        };
    }

    private static class TestObjectStorageService implements ObjectStorageService {

        private PresignedUrl presignedUrl = new PresignedUrl(
                URI.create("http://127.0.0.1:9000/default"),
                Instant.parse("2026-04-22T00:30:00Z")
        );

        @Override
        public StorageObject put(PutObjectCommand command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<StorageObject> get(GetObjectCommand command) {
            return Optional.empty();
        }

        @Override
        public void delete(DeleteObjectCommand command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PresignedUrl createPresignedUrl(PresignedUrlCommand command) {
            return presignedUrl;
        }
    }
}
