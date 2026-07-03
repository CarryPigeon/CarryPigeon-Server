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
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.ObjectProvider;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.Mention;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.port.MessageChannelBoundary;
import team.carrypigeon.backend.chat.domain.features.message.domain.port.MessageSenderSnapshot;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * NettyMessageRealtimePublisher 契约测试。
 * 职责：验证 realtime 下发时 file / voice 消息会派生可访问附件字段。
 * 边界：不验证完整 Netty 握手与鉴权链，只验证发布器出站载荷语义。
 */
@Tag("contract")
class NettyMessageRealtimePublisherTests {

    /**
     * 验证 file 消息 realtime 下发会转换为 share_key 与相对下载路径。
     */
    @Test
    @DisplayName("publish file message adds share key fields to realtime payload")
    void publish_fileMessage_addsShareKeyFieldsToRealtimePayload() {
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
                () -> 9001L,
                new MessageAttachmentPayloadResolver(objectProvider(storageService), jsonProvider)
        );

        publisher.publish(new ChannelMessage(
                5001L,
                "550e8400-e29b-41d4-a716-446655440000",
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
        ), senderSnapshot(1002L), List.of(1001L));

        TextWebSocketFrame frame = channel.readOutbound();

        assertNotNull(frame);
        var messageData = jsonProvider.readTree(frame.text()).path("data").path("payload").path("message").path("data");
        assertEquals("Core:File", jsonProvider.readTree(frame.text()).path("data").path("payload").path("message").path("domain").asText());
        assertEquals("shr_att_Y2hhbm5lbHMvMS9tZXNzYWdlcy9maWxlL2FjY291bnRzLzEwMDEvNTAwMS1kZW1vLnBkZg", messageData.path("share_key").asText());
        assertEquals("api/files/download/shr_att_Y2hhbm5lbHMvMS9tZXNzYWdlcy9maWxlL2FjY291bnRzLzEwMDEvNTAwMS1kZW1vLnBkZg", messageData.path("download_path").asText());
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
                () -> 9001L,
                new MessageAttachmentPayloadResolver(objectProvider(null), jsonProvider)
        );

        publisher.publish(new ChannelMessage(
                5002L,
                "550e8400-e29b-41d4-a716-446655440000",
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
        ), senderSnapshot(1001L), List.of(1001L));

        TextWebSocketFrame frame = channel.readOutbound();

        assertNotNull(frame);
        assertEquals("event", jsonProvider.readTree(frame.text()).path("type").asText());
        assertEquals("message.created", jsonProvider.readTree(frame.text()).path("data").path("event_type").asText());
    }

    /**
     * 验证撤回更新会通过独立的消息更新事件类型下发给在线成员。
     */
    @Test
    @DisplayName("publish update recalled message uses updated event type")
    void publishUpdate_recalledMessage_usesUpdatedEventType() {
        JsonProvider jsonProvider = jsonProvider();
        RealtimeSessionRegistry realtimeSessionRegistry = new RealtimeSessionRegistry();
        EmbeddedChannel channel = new EmbeddedChannel();
        realtimeSessionRegistry.register(1001L, channel);
        NettyMessageRealtimePublisher publisher = new NettyMessageRealtimePublisher(
                realtimeSessionRegistry,
                jsonProvider,
                new TimeProvider(Clock.fixed(Instant.parse("2026-04-22T00:00:00Z"), ZoneOffset.UTC)),
                () -> 9001L,
                new MessageAttachmentPayloadResolver(objectProvider(null), jsonProvider)
        );

        publisher.publishUpdate(new ChannelMessage(
                5010L,
                "550e8400-e29b-41d4-a716-446655440000",
                1L,
                1L,
                1001L,
                "text",
                "[消息已撤回]",
                "[消息已撤回]",
                null,
                null,
                null,
                "recalled",
                Instant.parse("2026-04-22T00:00:00Z")
        ), senderSnapshot(1001L), List.of(1001L));

        TextWebSocketFrame frame = channel.readOutbound();

        assertNotNull(frame);
        assertTrue(frame.text().contains("\"type\":\"event\""));
        assertTrue(frame.text().contains("\"event_type\":\"message.deleted\""));
        assertTrue(frame.text().contains("\"mid\":\"5010\""));
    }

    /**
     * 验证 `publishUpdate` 在 `editedMessage` 条件下满足 `usesMessageUpdatedEventType` 的测试契约。
     */
    @Test
    @DisplayName("publish update edited message uses message updated event type")
    void publishUpdate_editedMessage_usesMessageUpdatedEventType() {
        JsonProvider jsonProvider = jsonProvider();
        RealtimeSessionRegistry realtimeSessionRegistry = new RealtimeSessionRegistry();
        EmbeddedChannel channel = new EmbeddedChannel();
        realtimeSessionRegistry.register(1001L, channel);
        NettyMessageRealtimePublisher publisher = new NettyMessageRealtimePublisher(
                realtimeSessionRegistry,
                jsonProvider,
                new TimeProvider(Clock.fixed(Instant.parse("2026-04-22T00:10:00Z"), ZoneOffset.UTC)),
                () -> 9002L,
                new MessageAttachmentPayloadResolver(objectProvider(null), jsonProvider)
        );

        publisher.publishUpdate(new ChannelMessage(
                5011L,
                "550e8400-e29b-41d4-a716-446655440000",
                1L,
                1L,
                1001L,
                "text",
                "edited content",
                "edited content",
                "edited content",
                null,
                null,
                "[{\"type\":\"user\",\"uid\":\"1002\"}]",
                "{\"mid\":\"5000\",\"cid\":\"1\",\"uid\":\"1002\",\"preview\":\"hello\",\"send_time\":1700000000000}",
                "sent",
                Instant.parse("2026-04-22T00:00:00Z"),
                Instant.parse("2026-04-22T00:09:00Z"),
                2L
        ), senderSnapshot(1001L), List.of(1001L));

        TextWebSocketFrame frame = channel.readOutbound();

        assertNotNull(frame);
        var root = jsonProvider.readTree(frame.text());
        assertEquals("message.updated", root.path("data").path("event_type").asText());
        assertEquals(2L, root.path("data").path("payload").path("message").path("edit_version").asLong());
        assertEquals(1776816540000L, root.path("data").path("payload").path("message").path("edited_at").asLong());
        assertEquals("1002", root.path("data").path("payload").path("message").path("mentions").get(0).path("uid").asText());
        assertEquals("5000", root.path("data").path("payload").path("message").path("forwarded_from").path("mid").asText());
    }

    /**
     * 验证 `publishPin` 在 `emitsMessagePinnedEvent` 场景下的测试契约。
     */
    @Test
    @DisplayName("publish pin emits message pinned event")
    void publishPin_emitsMessagePinnedEvent() {
        JsonProvider jsonProvider = jsonProvider();
        RealtimeSessionRegistry realtimeSessionRegistry = new RealtimeSessionRegistry();
        EmbeddedChannel channel = new EmbeddedChannel();
        realtimeSessionRegistry.register(1001L, channel);
        NettyMessageRealtimePublisher publisher = new NettyMessageRealtimePublisher(
                realtimeSessionRegistry,
                jsonProvider,
                new TimeProvider(Clock.fixed(Instant.parse("2026-04-22T00:10:00Z"), ZoneOffset.UTC)),
                () -> 9003L,
                new MessageAttachmentPayloadResolver(objectProvider(null), jsonProvider)
        );

        publisher.publishPin(new MessageChannelBoundary.MessageChannelPin(7001L, 9L, 5001L, 1001L, "important", Instant.parse("2026-04-22T00:09:00Z")), List.of(1001L));

        TextWebSocketFrame frame = channel.readOutbound();

        assertNotNull(frame);
        var root = jsonProvider.readTree(frame.text());
        assertEquals("message.pinned", root.path("data").path("event_type").asText());
        assertEquals("7001", root.path("data").path("payload").path("pin_id").asText());
        assertEquals("5001", root.path("data").path("payload").path("mid").asText());
    }

    /**
     * 验证 `publishUnpin` 在 `emitsMessageUnpinnedEvent` 场景下的测试契约。
     */
    @Test
    @DisplayName("publish unpin emits message unpinned event")
    void publishUnpin_emitsMessageUnpinnedEvent() {
        JsonProvider jsonProvider = jsonProvider();
        RealtimeSessionRegistry realtimeSessionRegistry = new RealtimeSessionRegistry();
        EmbeddedChannel channel = new EmbeddedChannel();
        realtimeSessionRegistry.register(1001L, channel);
        NettyMessageRealtimePublisher publisher = new NettyMessageRealtimePublisher(
                realtimeSessionRegistry,
                jsonProvider,
                new TimeProvider(Clock.fixed(Instant.parse("2026-04-22T00:10:00Z"), ZoneOffset.UTC)),
                () -> 9004L,
                new MessageAttachmentPayloadResolver(objectProvider(null), jsonProvider)
        );

        publisher.publishUnpin(new MessageChannelBoundary.MessageChannelPin(7001L, 9L, 5001L, 1001L, "important", Instant.parse("2026-04-22T00:09:00Z")), 1001L, 1776816600000L, List.of(1001L));

        TextWebSocketFrame frame = channel.readOutbound();

        assertNotNull(frame);
        var root = jsonProvider.readTree(frame.text());
        assertEquals("message.unpinned", root.path("data").path("event_type").asText());
        assertEquals("7001", root.path("data").path("payload").path("pin_id").asText());
        assertEquals("1001", root.path("data").path("payload").path("unpinned_by_uid").asText());
    }

    /**
     * 验证 `publishMentionCreated` 在 `emitsMentionCreatedEvent` 场景下的测试契约。
     */
    @Test
    @DisplayName("publish mention created emits mention created event")
    void publishMentionCreated_emitsMentionCreatedEvent() {
        JsonProvider jsonProvider = jsonProvider();
        RealtimeSessionRegistry realtimeSessionRegistry = new RealtimeSessionRegistry();
        EmbeddedChannel channel = new EmbeddedChannel();
        realtimeSessionRegistry.register(1002L, channel);
        NettyMessageRealtimePublisher publisher = new NettyMessageRealtimePublisher(
                realtimeSessionRegistry,
                jsonProvider,
                new TimeProvider(Clock.fixed(Instant.parse("2026-04-22T00:10:00Z"), ZoneOffset.UTC)),
                () -> 9005L,
                new MessageAttachmentPayloadResolver(objectProvider(null), jsonProvider)
        );

        publisher.publishMentionCreated(new Mention(8001L, 9L, 5001L, 1001L, "user", 1002L, Instant.parse("2026-04-22T00:09:30Z"), false), List.of(1002L));

        TextWebSocketFrame frame = channel.readOutbound();

        assertNotNull(frame);
        var root = jsonProvider.readTree(frame.text());
        assertEquals("mention.created", root.path("data").path("event_type").asText());
        assertEquals("8001", root.path("data").path("payload").path("mention_id").asText());
        assertEquals("1002", root.path("data").path("payload").path("target").path("uid").asText());
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

    /**
     * `TestObjectStorageService` 测试辅助类型。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
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

    private static MessageSenderSnapshot senderSnapshot(long accountId) {
        return new MessageSenderSnapshot(accountId, "carry-user", "avatars/u/" + accountId + ".png");
    }
}
