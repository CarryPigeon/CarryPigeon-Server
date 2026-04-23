package team.carrypigeon.backend.chat.domain.features.message.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.features.message.application.command.SendChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.command.SendChannelTextMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.command.UploadChannelMessageAttachmentCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.draft.FileChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.application.draft.TextChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelMessageAttachmentUploadResult;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelMessageHistoryResult;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelMessageSearchResult;
import team.carrypigeon.backend.chat.domain.features.message.application.query.GetChannelMessageHistoryQuery;
import team.carrypigeon.backend.chat.domain.features.message.application.query.SearchChannelMessagesQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.MessageRealtimePublisher;
import team.carrypigeon.backend.chat.domain.features.message.support.attachment.MessageAttachmentObjectKeyPolicy;
import team.carrypigeon.backend.chat.domain.features.message.support.payload.MessageAttachmentPayloadResolver;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.ChannelMessagePluginRegistry;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.FileChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.TextChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.VoiceChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.server.config.ServerIdentityProperties;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.DeleteObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.GetObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PresignedUrl;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PresignedUrlCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PutObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.StorageObject;
import team.carrypigeon.backend.infrastructure.service.storage.api.service.ObjectStorageService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MessageApplicationService 契约测试。
 * 职责：验证频道消息发送、历史查询、搜索与附件上传用例的应用层编排契约。
 * 边界：不验证 HTTP、Netty 和真实数据库访问，只使用内存替身验证业务语义。
 */
class MessageApplicationServiceTests {

    private static final Instant BASE_TIME = Instant.parse("2026-04-22T00:00:00Z");

    /**
     * 验证发送文本消息时会持久化并使用同一个 messageId 进行实时分发。
     */
    @Test
    @DisplayName("send channel text message valid command persists and publishes same message id")
    void sendChannelTextMessage_validCommand_persistsAndPublishesSameMessageId() {
        Fixture fixture = new Fixture(null);

        ChannelMessageResult result = fixture.service.sendChannelTextMessage(
                new SendChannelTextMessageCommand(1001L, 1L, "hello world")
        );

        assertEquals(5001L, result.messageId());
        assertEquals(5001L, fixture.messageRepository.savedMessages.getFirst().messageId());
        assertEquals(5001L, fixture.publisher.publishedMessages.getFirst().messageId());
        assertEquals("carrypigeon-local", result.serverId());
        assertEquals("hello world", result.body());
        assertEquals("hello world", result.previewText());
        assertIterableEquals(List.of(1001L, 1002L), fixture.publisher.recipientAccountIds.getFirst());
    }

    /**
     * 验证通用消息发送入口在 text 草稿场景下保持既有 text 消息语义。
     */
    @Test
    @DisplayName("send channel message text draft preserves text semantics")
    void sendChannelMessage_textDraft_preservesTextSemantics() {
        Fixture fixture = new Fixture(null);

        ChannelMessageResult result = fixture.service.sendChannelMessage(
                new SendChannelMessageCommand(1001L, 1L, new TextChannelMessageDraft("hello plugin world"))
        );

        assertEquals("text", result.messageType());
        assertEquals("hello plugin world", result.body());
        assertEquals("hello plugin world", result.previewText());
        assertEquals(null, result.payload());
        assertEquals(null, result.metadata());
        assertEquals("sent", result.status());
    }

    /**
     * 验证 file 消息发送结果会派生临时访问 URL，同时持久化消息仍保持 canonical payload。
     */
    @Test
    @DisplayName("send channel message file draft returns payload with access url")
    void sendChannelMessage_fileDraft_returnsPayloadWithAccessUrl() {
        TestObjectStorageService storageService = new TestObjectStorageService();
        storageService.getResult = Optional.of(StorageObject.metadata("channels/1/messages/file/accounts/1001/5001-demo.pdf", "application/pdf", 1024L));
        storageService.presignedUrl = new PresignedUrl(
                URI.create("http://127.0.0.1:9000/file/demo.pdf?token=abc"),
                BASE_TIME.plusSeconds(1800)
        );
        Fixture fixture = new Fixture(storageService);

        ChannelMessageResult result = fixture.service.sendChannelMessage(new SendChannelMessageCommand(
                1001L,
                1L,
                new FileChannelMessageDraft(
                        "项目文档",
                        "channels/1/messages/file/accounts/1001/5001-demo.pdf",
                        "demo.pdf",
                        "application/pdf",
                        1024L,
                        null
                )
        ));

        assertEquals("file", result.messageType());
        assertEquals(
                "channels/1/messages/file/accounts/1001/5001-demo.pdf",
                fixture.jsonProvider.readTree(result.payload()).path("object_key").asText()
        );
        assertEquals(
                "http://127.0.0.1:9000/file/demo.pdf?token=abc",
                fixture.jsonProvider.readTree(result.payload()).path("access_url").asText()
        );
        assertEquals(
                "channels/1/messages/file/accounts/1001/5001-demo.pdf",
                fixture.jsonProvider.readTree(fixture.messageRepository.savedMessages.getFirst().payload()).path("object_key").asText()
        );
        assertTrue(fixture.jsonProvider.readTree(fixture.messageRepository.savedMessages.getFirst().payload()).path("access_url").isMissingNode());
    }

    /**
     * 验证非成员发送消息时会返回权限问题语义。
     */
    @Test
    @DisplayName("send channel text message non member throws forbidden problem")
    void sendChannelTextMessage_nonMember_throwsForbiddenProblem() {
        Fixture fixture = new Fixture(null);
        fixture.channelMemberRepository.memberships.clear();

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> fixture.service.sendChannelTextMessage(new SendChannelTextMessageCommand(1001L, 1L, "hello world"))
        );

        assertEquals("channel membership is required", exception.getMessage());
    }

    /**
     * 验证附件上传成功后会返回 canonical objectKey 与存储元信息。
     */
    @Test
    @DisplayName("upload channel message attachment valid command returns canonical attachment result")
    void uploadChannelMessageAttachment_validCommand_returnsCanonicalAttachmentResult() {
        TestObjectStorageService storageService = new TestObjectStorageService();
        storageService.putResult = StorageObject.metadata(
                "channels/1/messages/file/accounts/1001/5001-demo.pdf",
                "application/pdf",
                123L
        );
        Fixture fixture = new Fixture(storageService);

        ChannelMessageAttachmentUploadResult result = fixture.service.uploadChannelMessageAttachment(
                new UploadChannelMessageAttachmentCommand(
                        1001L,
                        1L,
                        "file",
                        "demo.pdf",
                        "application/pdf",
                        123L,
                        new ByteArrayInputStream("demo-content".getBytes())
                )
        );

        assertEquals("channels/1/messages/file/accounts/1001/5001-demo.pdf", result.objectKey());
        assertEquals("demo.pdf", result.filename());
        assertEquals("application/pdf", result.mimeType());
        assertEquals(123L, result.size());
        assertNotNull(storageService.lastPutCommand);
        assertEquals("channels/1/messages/file/accounts/1001/5001-demo.pdf", storageService.lastPutCommand.objectKey());
    }

    /**
     * 验证非法 messageType 会被识别为参数错误。
     */
    @Test
    @DisplayName("upload channel message attachment invalid type throws validation problem")
    void uploadChannelMessageAttachment_invalidType_throwsValidationProblem() {
        Fixture fixture = new Fixture(new TestObjectStorageService());

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> fixture.service.uploadChannelMessageAttachment(new UploadChannelMessageAttachmentCommand(
                        1001L,
                        1L,
                        "image",
                        "demo.png",
                        "image/png",
                        64L,
                        new ByteArrayInputStream(new byte[]{1, 2, 3})
                ))
        );

        assertEquals("messageType must be file or voice", exception.getMessage());
    }

    /**
     * 验证非成员上传附件时会返回权限问题语义。
     */
    @Test
    @DisplayName("upload channel message attachment non member throws forbidden problem")
    void uploadChannelMessageAttachment_nonMember_throwsForbiddenProblem() {
        Fixture fixture = new Fixture(new TestObjectStorageService());
        fixture.channelMemberRepository.memberships.clear();

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> fixture.service.uploadChannelMessageAttachment(new UploadChannelMessageAttachmentCommand(
                        1001L,
                        1L,
                        "file",
                        "demo.pdf",
                        "application/pdf",
                        64L,
                        new ByteArrayInputStream(new byte[]{1, 2, 3})
                ))
        );

        assertEquals("channel membership is required", exception.getMessage());
    }

    /**
     * 验证按频道搜索消息时会返回匹配关键字的消息。
     */
    @Test
    @DisplayName("search channel messages valid query returns matching messages")
    void searchChannelMessages_validQuery_returnsMatchingMessages() {
        Fixture fixture = new Fixture(null);
        fixture.messageRepository.searchResults.add(new ChannelMessage(
                5003L, "carrypigeon-local", 1L, 1L, 1002L, "text",
                "hello body", "[文本消息] hello body", "hello body", null, null, "sent", BASE_TIME.plusSeconds(2)
        ));

        ChannelMessageSearchResult result = fixture.service.searchChannelMessages(
                new SearchChannelMessagesQuery(1001L, 1L, "hello", 20)
        );

        assertEquals(1, result.messages().size());
        assertEquals("[文本消息] hello body", result.messages().getFirst().previewText());
    }

    /**
     * 验证 file 历史消息读取时会派生临时访问 URL。
     */
    @Test
    @DisplayName("get channel message history file message returns payload with access url")
    void getChannelMessageHistory_fileMessage_returnsPayloadWithAccessUrl() {
        TestObjectStorageService storageService = new TestObjectStorageService();
        storageService.presignedUrl = new PresignedUrl(
                URI.create("http://127.0.0.1:9000/file/demo.pdf?token=abc"),
                BASE_TIME.plusSeconds(1800)
        );
        Fixture fixture = new Fixture(storageService);
        fixture.messageRepository.history.add(new ChannelMessage(
                5002L,
                "carrypigeon-local",
                1L,
                1L,
                1002L,
                "file",
                "项目文档",
                "[文件消息] demo.pdf",
                "项目文档 demo.pdf",
                fixture.jsonProvider.toJson(Map.of(
                        "object_key", "channels/1/messages/file/accounts/1002/5001-demo.pdf",
                        "filename", "demo.pdf",
                        "mime_type", "application/pdf",
                        "size", 1024L
                )),
                null,
                "sent",
                BASE_TIME.plusSeconds(1)
        ));

        ChannelMessageHistoryResult result = fixture.service.getChannelMessageHistory(
                new GetChannelMessageHistoryQuery(1001L, 1L, null, 20)
        );

        assertEquals(1, result.messages().size());
        assertEquals(
                "http://127.0.0.1:9000/file/demo.pdf?token=abc",
                fixture.jsonProvider.readTree(result.messages().getFirst().payload()).path("access_url").asText()
        );
        assertEquals(
                "channels/1/messages/file/accounts/1002/5001-demo.pdf",
                fixture.jsonProvider.readTree(result.messages().getFirst().payload()).path("object_key").asText()
        );
    }

    /**
     * 验证按频道查询历史消息时会返回倒序消息和下一页游标。
     */
    @Test
    @DisplayName("get channel message history member returns messages and next cursor")
    void getChannelMessageHistory_member_returnsMessagesAndNextCursor() {
        Fixture fixture = new Fixture(null);
        fixture.messageRepository.history.add(new ChannelMessage(
                5002L, "carrypigeon-local", 1L, 1L, 1002L, "text", "second", "[文本消息] second", "second", null, null, "sent", BASE_TIME.plusSeconds(1)
        ));
        fixture.messageRepository.history.add(new ChannelMessage(
                5001L, "carrypigeon-local", 1L, 1L, 1001L, "text", "first", "[文本消息] first", "first", null, null, "sent", BASE_TIME
        ));

        ChannelMessageHistoryResult result = fixture.service.getChannelMessageHistory(
                new GetChannelMessageHistoryQuery(1001L, 1L, null, 20)
        );

        assertEquals(2, result.messages().size());
        assertEquals(5002L, result.messages().getFirst().messageId());
        assertEquals(5001L, result.nextCursor());
    }

    /**
     * 验证频道不存在时历史查询会返回不存在问题语义。
     */
    @Test
    @DisplayName("get channel message history missing channel throws not found problem")
    void getChannelMessageHistory_missingChannel_throwsNotFoundProblem() {
        Fixture fixture = new Fixture(null);
        fixture.channelRepository.channel = null;

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> fixture.service.getChannelMessageHistory(new GetChannelMessageHistoryQuery(1001L, 9L, null, 20))
        );

        assertEquals("channel does not exist", exception.getMessage());
    }

    private static class Fixture {

        private final InMemoryChannelRepository channelRepository = new InMemoryChannelRepository();
        private final InMemoryChannelMemberRepository channelMemberRepository = new InMemoryChannelMemberRepository();
        private final InMemoryMessageRepository messageRepository = new InMemoryMessageRepository();
        private final RecordingMessageRealtimePublisher publisher = new RecordingMessageRealtimePublisher();
        private final JsonProvider jsonProvider = jsonProvider();
        private final MessageApplicationService service;

        private Fixture(ObjectStorageService storageService) {
            channelRepository.channel = new Channel(1L, 1L, "public", "public", true, BASE_TIME, BASE_TIME);
            channelMemberRepository.memberships.put(1L, List.of(1001L, 1002L));
            ObjectProvider<ObjectStorageService> objectStorageServiceProvider = objectProvider(storageService);
            MessageAttachmentPayloadResolver payloadResolver = new MessageAttachmentPayloadResolver(
                    objectStorageServiceProvider,
                    jsonProvider
            );
            MessageAttachmentObjectKeyPolicy objectKeyPolicy = new MessageAttachmentObjectKeyPolicy();
            this.service = new MessageApplicationService(
                    channelRepository,
                    channelMemberRepository,
                    messageRepository,
                    publisher,
                    channelMessagePluginRegistry(storageService, jsonProvider, objectKeyPolicy),
                    objectKeyPolicy,
                    payloadResolver,
                    new ServerIdentityProperties("carrypigeon-local"),
                    new FixedIdGenerator(),
                    new TimeProvider(Clock.fixed(BASE_TIME, ZoneOffset.UTC)),
                    new NoopTransactionRunner(),
                    objectStorageServiceProvider
            );
        }
    }

    private static ChannelMessagePluginRegistry channelMessagePluginRegistry(
            ObjectStorageService storageService,
            JsonProvider jsonProvider,
            MessageAttachmentObjectKeyPolicy objectKeyPolicy
    ) {
        List<team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePlugin> plugins = new ArrayList<>();
        plugins.add(new TextChannelMessagePlugin());
        if (storageService != null) {
            plugins.add(new FileChannelMessagePlugin(storageService, jsonProvider, objectKeyPolicy));
            plugins.add(new VoiceChannelMessagePlugin(storageService, jsonProvider, objectKeyPolicy));
        }
        return new ChannelMessagePluginRegistry(plugins);
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

    private static class InMemoryChannelRepository implements ChannelRepository {

        private Channel channel;

        @Override
        public Optional<Channel> findDefaultChannel() {
            return Optional.ofNullable(channel);
        }

        @Override
        public Optional<Channel> findById(long channelId) {
            return Optional.ofNullable(channel != null && channel.id() == channelId ? channel : null);
        }
    }

    private static class InMemoryChannelMemberRepository implements ChannelMemberRepository {

        private final Map<Long, List<Long>> memberships = new HashMap<>();

        @Override
        public boolean exists(long channelId, long accountId) {
            return memberships.getOrDefault(channelId, List.of()).contains(accountId);
        }

        @Override
        public void save(team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember channelMember) {
            memberships.computeIfAbsent(channelMember.channelId(), ignored -> new ArrayList<>()).add(channelMember.accountId());
        }

        @Override
        public List<Long> findAccountIdsByChannelId(long channelId) {
            return memberships.getOrDefault(channelId, List.of());
        }
    }

    private static class InMemoryMessageRepository implements MessageRepository {

        private final List<ChannelMessage> savedMessages = new ArrayList<>();
        private final List<ChannelMessage> history = new ArrayList<>();
        private final List<ChannelMessage> searchResults = new ArrayList<>();

        @Override
        public ChannelMessage save(ChannelMessage message) {
            savedMessages.add(message);
            return message;
        }

        @Override
        public List<ChannelMessage> findByChannelIdBefore(long channelId, Long cursorMessageId, int limit) {
            return history;
        }

        @Override
        public List<ChannelMessage> searchByChannelId(long channelId, String keyword, int limit) {
            return searchResults;
        }
    }

    private static class RecordingMessageRealtimePublisher implements MessageRealtimePublisher {

        private final List<ChannelMessage> publishedMessages = new ArrayList<>();
        private final List<List<Long>> recipientAccountIds = new ArrayList<>();

        @Override
        public void publish(ChannelMessage message, java.util.Collection<Long> recipients) {
            publishedMessages.add(message);
            recipientAccountIds.add(List.copyOf(recipients));
        }
    }

    private static class FixedIdGenerator implements IdGenerator {

        @Override
        public long nextLongId() {
            return 5001L;
        }
    }

    private static class NoopTransactionRunner implements TransactionRunner {

        @Override
        public <T> T runInTransaction(java.util.function.Supplier<T> action) {
            return action.get();
        }

        @Override
        public void runInTransaction(Runnable action) {
            action.run();
        }
    }

    private static class TestObjectStorageService implements ObjectStorageService {

        private StorageObject putResult = StorageObject.metadata("channels/1/messages/file/accounts/1001/5001-demo.pdf", "application/pdf", 128L);
        private Optional<StorageObject> getResult = Optional.empty();
        private PresignedUrl presignedUrl = new PresignedUrl(URI.create("http://127.0.0.1:9000/default"), BASE_TIME.plusSeconds(1800));
        private PutObjectCommand lastPutCommand;

        @Override
        public StorageObject put(PutObjectCommand command) {
            this.lastPutCommand = command;
            return putResult;
        }

        @Override
        public Optional<StorageObject> get(GetObjectCommand command) {
            return getResult;
        }

        @Override
        public void delete(DeleteObjectCommand command) {
        }

        @Override
        public PresignedUrl createPresignedUrl(PresignedUrlCommand command) {
            return presignedUrl;
        }
    }
}
