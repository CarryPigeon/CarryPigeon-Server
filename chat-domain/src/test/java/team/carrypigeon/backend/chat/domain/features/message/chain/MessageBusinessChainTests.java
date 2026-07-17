package team.carrypigeon.backend.chat.domain.features.message.chain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.HandlerInterceptor;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelAuditLog;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelPin;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelAuditLogRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelPinRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelGovernancePolicy;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.ChannelMessageController;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.ChannelPinsController;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.MentionController;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.MessageController;
import team.carrypigeon.backend.chat.domain.features.message.controller.support.ChannelMessageV1ResponseMapper;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.Mention;
import team.carrypigeon.backend.chat.domain.features.message.domain.port.ChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.domain.port.MessageChannelBoundary;
import team.carrypigeon.backend.chat.domain.features.message.domain.port.MessageRealtimePublisher;
import team.carrypigeon.backend.chat.domain.features.message.domain.port.MessageSenderSnapshot;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MentionRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessageAttachmentDomainApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessageLifecycleDomainApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePluginDescriptor;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePluginRegistration;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePluginRegistry;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePublishingDomainApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessageTimelineDomainApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelPinDomainApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.MentionDomainApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.MessageAttachmentObjectKeyPolicy;
import team.carrypigeon.backend.chat.domain.features.message.support.channel.ChannelBackedMessageChannelBoundary;
import team.carrypigeon.backend.chat.domain.features.message.support.payload.MessageAttachmentPayloadResolver;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.FileChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.TextChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.VoiceChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.server.config.ServerIdentityProperties;
import team.carrypigeon.backend.chat.domain.features.user.domain.api.UserProfileApi;
import team.carrypigeon.backend.chat.domain.features.user.domain.command.GetCurrentUserProfileCommand;
import team.carrypigeon.backend.chat.domain.features.user.domain.command.GetUserProfileByAccountIdCommand;
import team.carrypigeon.backend.chat.domain.features.user.domain.command.UpdateCurrentUserEmailCommand;
import team.carrypigeon.backend.chat.domain.features.user.domain.command.UpdateCurrentUserProfileCommand;
import team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile;
import team.carrypigeon.backend.chat.domain.features.user.domain.projection.UserProfilePageResult;
import team.carrypigeon.backend.chat.domain.features.user.domain.projection.UserProfileResult;
import team.carrypigeon.backend.chat.domain.features.user.domain.query.GetUserProfilesQuery;
import team.carrypigeon.backend.chat.domain.features.user.domain.query.SearchUserProfilesQuery;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
import team.carrypigeon.backend.chat.domain.shared.controller.OpaqueCursorCodec;
import team.carrypigeon.backend.chat.domain.shared.controller.advice.GlobalExceptionHandler;
import team.carrypigeon.backend.chat.domain.shared.controller.support.RequestAuthenticationContext;
import team.carrypigeon.backend.chat.domain.shared.domain.auth.AuthenticatedAccount;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 消息真实业务链路测试。
 * 职责：验证消息 HTTP 入口、真实领域 API、频道边界、消息插件、提及、置顶和附件端口之间的业务闭环。
 * 边界：不连接 MySQL、Redis、MinIO 或 WebSocket；外部服务以确定性内存替身隔离。
 */
@Tag("business")
class MessageBusinessChainTests {

    private static final Instant BASE_TIME = Instant.parse("2026-04-23T08:00:00Z");

    /**
     * 验证发送文本消息后，历史、搜索、仓储和实时发布都能读取同一条领域消息。
     */
    @Test
    @DisplayName("send text message is visible in history search repository and realtime publisher")
    void sendTextMessage_validRequest_visibleInHistorySearchRepositoryAndRealtimePublisher() throws Exception {
        Fixture fixture = new Fixture();

        MvcResult sendResult = fixture.mvc(1001L).perform(post("/api/channels/1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "domain":"Core:Text",
                                  "domain_version":"1.0.0",
                                  "data":{"text":"hello business chain"}
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mid").value("7001"))
                .andExpect(jsonPath("$.cid").value("1"))
                .andExpect(jsonPath("$.uid").value("1001"))
                .andExpect(jsonPath("$.sender.nickname").value("carry-user-1001"))
                .andExpect(jsonPath("$.domain").value("Core:Text"))
                .andExpect(jsonPath("$.data.text").value("hello business chain"))
                .andReturn();
        long messageId = Long.parseLong(fixture.readJson(sendResult).path("mid").asText());

        fixture.mvc(1001L).perform(get("/api/channels/1/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].mid").value(String.valueOf(messageId)))
                .andExpect(jsonPath("$.items[0].data.text").value("hello business chain"))
                .andExpect(jsonPath("$.has_more").value(false));

        fixture.mvc(1002L).perform(get("/api/channels/1/messages/search").param("q", "business"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].mid").value(String.valueOf(messageId)))
                .andExpect(jsonPath("$.items[0].preview").value("hello business chain"));

        assertEquals("hello business chain", fixture.messageRepository.messagesById.get(messageId).body());
        assertEquals(1, fixture.publisher.publishedMessages.size());
        assertEquals(List.of(1001L, 1002L), fixture.publisher.recipientAccountIds.get(0));
    }

    /**
     * 验证文本消息写入 mention 后，被提及用户可以查询、单条已读和批量已读。
     */
    @Test
    @DisplayName("mention message creates inbox item and read state can be updated")
    void sendTextMessage_withMention_createsInboxItemAndReadStateCanBeUpdated() throws Exception {
        Fixture fixture = new Fixture();

        fixture.mvc(1001L).perform(post("/api/channels/1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "domain":"Core:Text",
                                  "domain_version":"1.0.0",
                                  "data":{"text":"hello @1002"},
                                  "mentions":[{"type":"user","uid":"1002"}]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mentions[0].uid").value("1002"));

        fixture.mvc(1002L).perform(get("/api/mentions").param("unread_only", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].mention_id").value("7002"))
                .andExpect(jsonPath("$.items[0].from_uid").value("1001"))
                .andExpect(jsonPath("$.items[0].target.uid").value("1002"))
                .andExpect(jsonPath("$.items[0].read").value(false));

        fixture.mvc(1002L).perform(put("/api/mentions/7002/read"))
                .andExpect(status().isNoContent());
        assertTrue(fixture.mentionRepository.mentions.get(0).read());

        fixture.mvc(1001L).perform(post("/api/channels/1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "domain":"Core:Text",
                                  "domain_version":"1.0.0",
                                  "data":{"text":"hello again @1002"},
                                  "mentions":[{"type":"user","uid":"1002"}]
                                }
                                """))
                .andExpect(status().isCreated());
        fixture.mvc(1002L).perform(put("/api/mentions/read_state")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cid":"1"}
                                """))
                .andExpect(status().isNoContent());

        assertTrue(fixture.mentionRepository.mentions.stream().allMatch(Mention::read));
        assertEquals(2, fixture.publisher.createdMentions.size());
    }

    /**
     * 验证编辑会更新消息正文和编辑版本，随后删除会移除仓储记录并发布更新事件。
     */
    @Test
    @DisplayName("edit and delete message update repository version and publish update event")
    void editAndDeleteMessage_ownedMessage_updatesRepositoryVersionAndPublishesUpdateEvent() throws Exception {
        Fixture fixture = new Fixture();
        long messageId = fixture.sendText(1001L, "before edit");

        fixture.mvc(1001L).perform(patch("/api/messages/{messageId}", messageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "domain":"Core:Text",
                                  "domain_version":"1.0.0",
                                  "data":{"text":"after edit"},
                                  "expected_edit_version":1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mid").value(String.valueOf(messageId)))
                .andExpect(jsonPath("$.data.text").value("after edit"))
                .andExpect(jsonPath("$.edit_version").value(2));

        ChannelMessage updated = fixture.messageRepository.messagesById.get(messageId);
        assertEquals("after edit", updated.body());
        assertEquals(2L, updated.editVersion());

        fixture.mvc(1001L).perform(delete("/api/messages/{messageId}", messageId))
                .andExpect(status().isNoContent());

        assertFalse(fixture.messageRepository.messagesById.containsKey(messageId));
        assertEquals(1, fixture.publisher.updatedMessages.size());
        assertEquals(1, fixture.publisher.deletedMessages.size());
        assertEquals(messageId, fixture.publisher.deletedMessages.getFirst().messageId());
    }

    /**
     * 验证附件上传返回的 object_key 可继续发送文件消息，并在响应中转换为 share_key 下载语义。
     */
    @Test
    @DisplayName("upload attachment then send file message returns share key data")
    void uploadAttachmentThenSendFileMessage_validFile_returnsShareKeyData() throws Exception {
        Fixture fixture = new Fixture();
        MockMultipartFile file = new MockMultipartFile("file", "demo.pdf", "application/pdf", "demo-content".getBytes());

        MvcResult uploadResult = fixture.mvc(1001L).perform(multipart("/api/channels/1/messages/attachments")
                        .file(file)
                        .param("message_type", "file"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.message").doesNotExist())
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.filename").value("demo.pdf"))
                .andExpect(jsonPath("$.mime_type").value("application/pdf"))
                .andReturn();
        JsonNode uploadJson = fixture.readJson(uploadResult);
        String objectKey = uploadJson.path("object_key").asText();
        String shareKey = uploadJson.path("share_key").asText();

        fixture.mvc(1001L).perform(post("/api/channels/1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "domain":"Core:File",
                                  "domain_version":"1.0.0",
                                  "data":{
                                    "object_key":"%s",
                                    "filename":"demo.pdf",
                                    "mime_type":"application/pdf",
                                    "size":12,
                                    "text":"read this"
                                  }
                                }
                                """.formatted(objectKey)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.domain").value("Core:File"))
                .andExpect(jsonPath("$.data.share_key").value(shareKey))
                .andExpect(jsonPath("$.data.download_path").value("/api/files/download/" + shareKey))
                .andExpect(jsonPath("$.data.object_key").doesNotExist());

        assertEquals(objectKey, fixture.storageService.lastPutCommand.objectKey());
        assertTrue(fixture.storageService.objects.containsKey(objectKey));
    }

    /**
     * 验证频道 owner 能置顶、查询和取消置顶消息，且置顶事件通过消息实时端口发出。
     */
    @Test
    @DisplayName("pin list and unpin message follow channel moderation rules")
    void pinListAndUnpinMessage_channelOwner_updatesPinStateAndPublishesEvents() throws Exception {
        Fixture fixture = new Fixture();
        long messageId = fixture.sendText(1001L, "important notice");

        fixture.mvc(1001L).perform(post("/api/channels/1/pins/{messageId}", messageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"note":"read first"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cid").value("1"))
                .andExpect(jsonPath("$.mid").value(String.valueOf(messageId)))
                .andExpect(jsonPath("$.pinned_by_uid").value("1001"))
                .andExpect(jsonPath("$.note").value("read first"));

        fixture.mvc(1002L).perform(get("/api/channels/1/pins"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].mid").value(String.valueOf(messageId)))
                .andExpect(jsonPath("$.has_more").value(false));

        fixture.mvc(1001L).perform(delete("/api/channels/1/pins/{messageId}", messageId))
                .andExpect(status().isNoContent());

        assertEquals(0, fixture.channelPinRepository.pins.size());
        assertEquals(1, fixture.publisher.pinnedMessages.size());
        assertEquals(1, fixture.publisher.unpinnedMessages.size());
    }

    /**
     * 验证非频道成员不能发送、读取或搜索频道消息。
     */
    @Test
    @DisplayName("non member cannot send read or search channel messages")
    void channelMessageAccess_nonMember_returnsForbidden() throws Exception {
        Fixture fixture = new Fixture();

        fixture.mvc(1003L).perform(post("/api/channels/1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"domain":"Core:Text","domain_version":"1.0.0","data":{"text":"blocked"}}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.reason").value("not_channel_member"));

        fixture.mvc(1003L).perform(get("/api/channels/1/messages"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.reason").value("not_channel_member"));

        fixture.mvc(1003L).perform(get("/api/channels/1/messages/search").param("q", "blocked"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.reason").value("not_channel_member"));
    }

    /**
     * 验证消息协议参数错误会在真实 HTTP 链路上返回稳定校验错误。
     */
    @Test
    @DisplayName("invalid message requests return validation errors")
    void messageRequests_invalidPayload_returnsValidationErrors() throws Exception {
        Fixture fixture = new Fixture();

        fixture.mvc(1001L).perform(post("/api/channels/1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"domain":"Core:Text","domain_version":"1.0.0","data":{"text":"   "}}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.reason").value("validation_failed"));

        fixture.mvc(1001L).perform(get("/api/channels/1/messages/search"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.message").value("q must not be blank"));

        long messageId = fixture.sendText(1001L, "versioned");
        fixture.mvc(1001L).perform(patch("/api/messages/{messageId}", messageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "domain":"Core:Text",
                                  "domain_version":"1.0.0",
                                  "data":{"text":"version conflict"},
                                  "expected_edit_version":99
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.reason").value("conflict"));
    }

    /**
     * 验证普通成员不能管理置顶，缺失消息也不能被置顶。
     */
    @Test
    @DisplayName("pin failures cover member permission and missing message")
    void pinChannelMessage_invalidOperatorOrMessage_returnsExpectedErrors() throws Exception {
        Fixture fixture = new Fixture();
        long messageId = fixture.sendText(1001L, "pin candidate");

        fixture.mvc(1002L).perform(post("/api/channels/1/pins/{messageId}", messageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"note":"member cannot pin"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.reason").value("forbidden"));

        fixture.mvc(1001L).perform(post("/api/channels/1/pins/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"note":"missing"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.reason").value("not_found"));
    }

    /**
     * 验证转发消息会保留源消息摘要并在目标频道生成新消息。
     */
    @Test
    @DisplayName("forward message creates new message with source summary")
    void forwardMessage_existingMessage_createsNewMessageWithSourceSummary() throws Exception {
        Fixture fixture = new Fixture();
        long sourceMessageId = fixture.sendText(1001L, "source body");

        fixture.mvc(1002L).perform(post("/api/messages/{messageId}/forward", sourceMessageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"target_cid":"1","comment":"please check"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mid").value("7002"))
                .andExpect(jsonPath("$.uid").value("1002"))
                .andExpect(jsonPath("$.data.text").value("please check\n\n[Forwarded] source body"))
                .andExpect(jsonPath("$.forwarded_from.mid").value(String.valueOf(sourceMessageId)))
                .andExpect(jsonPath("$.forwarded_from.cid").value("1"))
                .andExpect(jsonPath("$.forwarded_from.uid").value("1001"))
                .andExpect(jsonPath("$.forwarded_from.preview").value("source body"));

        assertEquals(2, fixture.messageRepository.messagesById.size());
        assertEquals(2, fixture.publisher.publishedMessages.size());
    }

    /**
     * 验证语音附件上传后可发送 voice 消息，并返回 share_key 与语音元数据。
     */
    @Test
    @DisplayName("upload voice attachment then send voice message returns voice metadata")
    void uploadVoiceAttachmentThenSendVoiceMessage_validVoice_returnsVoiceMetadata() throws Exception {
        Fixture fixture = new Fixture();
        MockMultipartFile voice = new MockMultipartFile("file", "voice.mp3", "audio/mpeg", "voice-content".getBytes());

        MvcResult uploadResult = fixture.mvc(1001L).perform(multipart("/api/channels/1/messages/attachments")
                        .file(voice)
                        .param("message_type", "voice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.message").doesNotExist())
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.filename").value("voice.mp3"))
                .andExpect(jsonPath("$.mime_type").value("audio/mpeg"))
                .andReturn();
        JsonNode uploadJson = fixture.readJson(uploadResult);
        String objectKey = uploadJson.path("object_key").asText();
        String shareKey = uploadJson.path("share_key").asText();

        fixture.mvc(1001L).perform(post("/api/channels/1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "domain":"Core:Voice",
                                  "domain_version":"1.0.0",
                                  "data":{
                                    "object_key":"%s",
                                    "filename":"voice.mp3",
                                    "mime_type":"audio/mpeg",
                                    "size":13,
                                    "duration_millis":2300,
                                    "transcript":"voice transcript"
                                  }
                                }
                                """.formatted(objectKey)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.domain").value("Core:Voice"))
                .andExpect(jsonPath("$.data.share_key").value(shareKey))
                .andExpect(jsonPath("$.data.duration_millis").value(2300))
                .andExpect(jsonPath("$.data.transcript").value("voice transcript"))
                .andExpect(jsonPath("$.preview").value("[语音消息] voice.mp3 2s"));
    }

    /**
     * 验证 around_mid 查询会按 before/after 返回目标消息附近窗口。
     */
    @Test
    @DisplayName("history around message returns neighbor window")
    void getChannelMessages_aroundMessage_returnsNeighborWindow() throws Exception {
        Fixture fixture = new Fixture();
        long first = fixture.sendText(1001L, "first");
        long middle = fixture.sendText(1001L, "middle");
        long last = fixture.sendText(1001L, "last");

        fixture.mvc(1002L).perform(get("/api/channels/1/messages")
                        .param("around_mid", String.valueOf(middle))
                        .param("before", "1")
                        .param("after", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].mid").value(String.valueOf(last)))
                .andExpect(jsonPath("$.items[1].mid").value(String.valueOf(middle)))
                .andExpect(jsonPath("$.items[2].mid").value(String.valueOf(first)))
                .andExpect(jsonPath("$.has_more").value(false));
    }

    /**
     * 验证 mention inbox 支持 unread、频道和游标过滤。
     */
    @Test
    @DisplayName("mention inbox filters unread channel and cursor")
    void listMentions_withFilters_returnsExpectedUnreadWindow() throws Exception {
        Fixture fixture = new Fixture();
        fixture.sendMention(1001L, 1002L, "mention one");
        fixture.sendMention(1001L, 1002L, "mention two");
        fixture.sendMention(1001L, 1002L, "mention three");
        fixture.mvc(1002L).perform(put("/api/mentions/7002/read"))
                .andExpect(status().isNoContent());
        String cursor = OpaqueCursorCodec.encode("mentions", 7006L);

        fixture.mvc(1002L).perform(get("/api/mentions")
                        .param("unread_only", "true")
                        .param("cid", "1")
                        .param("cursor", cursor)
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].mention_id").value("7004"))
                .andExpect(jsonPath("$.items[0].read").value(false));
    }

    /**
     * 验证被禁言的频道成员不能发送消息。
     */
    @Test
    @DisplayName("muted member cannot send message")
    void sendTextMessage_mutedMember_returnsForbidden() throws Exception {
        Fixture fixture = new Fixture();
        fixture.channelMemberRepository.update(new ChannelMember(
                1L,
                1002L,
                ChannelMemberRole.MEMBER,
                BASE_TIME,
                BASE_TIME.plusSeconds(60)
        ));

        fixture.mvc(1002L).perform(post("/api/channels/1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"domain":"Core:Text","domain_version":"1.0.0","data":{"text":"muted"}}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.reason").value("user_muted"));
    }

    /**
     * 验证被禁言成员也不能通过转发绕过目标频道发送治理。
     */
    @Test
    @DisplayName("muted member cannot forward message")
    void forwardMessage_mutedMember_returnsForbidden() throws Exception {
        Fixture fixture = new Fixture();
        long sourceMessageId = fixture.sendText(1001L, "source for muted forward");
        fixture.channelMemberRepository.update(new ChannelMember(
                1L,
                1002L,
                ChannelMemberRole.MEMBER,
                BASE_TIME,
                BASE_TIME.plusSeconds(60)
        ));

        fixture.mvc(1002L).perform(post("/api/messages/{messageId}/forward", sourceMessageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"target_cid":"1","comment":"muted forward"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.reason").value("user_muted"));

        assertEquals(1, fixture.messageRepository.messagesById.size());
    }

    /**
     * 验证普通成员不能删除他人消息，而 owner 可以删除成员消息。
     */
    @Test
    @DisplayName("delete message follows recall permission rules")
    void deleteMessage_otherSender_followsRecallPermissionRules() throws Exception {
        Fixture fixture = new Fixture();
        long ownerMessageId = fixture.sendText(1001L, "owner message");

        fixture.mvc(1002L).perform(delete("/api/messages/{messageId}", ownerMessageId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.reason").value("forbidden"));

        long memberMessageId = fixture.sendText(1002L, "member message");
        fixture.mvc(1001L).perform(delete("/api/messages/{messageId}", memberMessageId))
                .andExpect(status().isNoContent());

        assertFalse(fixture.messageRepository.messagesById.containsKey(memberMessageId));
        assertTrue(fixture.publisher.deletedMessages.stream()
                .anyMatch(message -> message.messageId() == memberMessageId && "recalled".equals(message.status())));
    }

    /**
     * 验证频道消息撤回 HTTP 入口会复用生命周期领域规则并发布更新事件。
     */
    @Test
    @DisplayName("recall message endpoint redacts message and publishes update")
    void recallChannelMessage_ownedMessage_redactsMessageAndPublishesUpdate() throws Exception {
        Fixture fixture = new Fixture();
        long messageId = fixture.sendText(1002L, "recall via http");

        fixture.mvc(1002L).perform(post("/api/channels/1/messages/{messageId}/recall", messageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mid").value(String.valueOf(messageId)))
                .andExpect(jsonPath("$.data.text").value("[消息已撤回]"));

        assertEquals("recalled", fixture.messageRepository.messagesById.get(messageId).status());
        assertTrue(fixture.publisher.updatedMessages.stream()
                .anyMatch(message -> message.messageId() == messageId && "recalled".equals(message.status())));
    }

    /**
     * 验证附件消息会拒绝越权 object_key 和不存在的对象。
     */
    @Test
    @DisplayName("attachment message rejects out of scope or missing object")
    void sendAttachmentMessage_invalidObjectKey_returnsExpectedErrors() throws Exception {
        Fixture fixture = new Fixture();
        MockMultipartFile file = new MockMultipartFile("file", "scoped.pdf", "application/pdf", "scoped-content".getBytes());
        MvcResult uploadResult = fixture.mvc(1001L).perform(multipart("/api/channels/1/messages/attachments")
                        .file(file)
                        .param("message_type", "file"))
                .andExpect(status().isOk())
                .andReturn();
        String ownerObjectKey = fixture.readJson(uploadResult).path("object_key").asText();

        fixture.mvc(1002L).perform(post("/api/channels/1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "domain":"Core:File",
                                  "domain_version":"1.0.0",
                                  "data":{"object_key":"%s","filename":"scoped.pdf"}
                                }
                                """.formatted(ownerObjectKey)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.message").value("file objectKey is out of allowed channel scope"));

        String missingObjectKey = "channels/1/messages/file/accounts/1001/9999-missing.pdf";
        fixture.mvc(1001L).perform(post("/api/channels/1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "domain":"Core:File",
                                  "domain_version":"1.0.0",
                                  "data":{"object_key":"%s","filename":"missing.pdf"}
                                }
                                """.formatted(missingObjectKey)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.reason").value("not_found"));
    }

    /**
     * 验证重复置顶会覆盖备注，超过频道置顶上限会返回校验错误。
     */
    @Test
    @DisplayName("pin duplicate overwrites note and pin limit is enforced")
    void pinChannelMessage_duplicateAndLimit_overwritesThenRejectsLimit() throws Exception {
        Fixture fixture = new Fixture();
        long messageId = fixture.sendText(1001L, "duplicate pin");

        fixture.mvc(1001L).perform(post("/api/channels/1/pins/{messageId}", messageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"note":"first"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.note").value("first"));
        fixture.mvc(1001L).perform(post("/api/channels/1/pins/{messageId}", messageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"note":"second"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.note").value("second"));
        assertEquals(1, fixture.channelPinRepository.pins.size());

        for (int index = 0; index < 49; index++) {
            long extraMessageId = fixture.sendText(1001L, "pin limit " + index);
            fixture.mvc(1001L).perform(post("/api/channels/1/pins/{messageId}", extraMessageId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"note":"limit"}
                                    """))
                    .andExpect(status().isOk());
        }
        long overflowMessageId = fixture.sendText(1001L, "overflow");
        fixture.mvc(1001L).perform(post("/api/channels/1/pins/{messageId}", overflowMessageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"note":"overflow"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.reason").value("pin_limit_reached"));
    }

    /**
     * 验证消息搜索会同时应用发送者、domain、before 和 after 过滤条件。
     */
    @Test
    @DisplayName("search messages applies sender domain and message bounds")
    void searchChannelMessages_withCombinedFilters_returnsMatchingWindow() throws Exception {
        Fixture fixture = new Fixture();
        long first = fixture.sendText(1001L, "filter first");
        fixture.sendText(1002L, "filter member");
        long target = fixture.sendText(1001L, "filter target");
        long later = fixture.sendText(1001L, "filter later");

        fixture.mvc(1002L).perform(get("/api/channels/1/messages/search")
                        .param("q", "filter")
                        .param("sender_uid", "1001")
                        .param("domain", "Core:Text")
                        .param("after_mid", String.valueOf(first))
                        .param("before_mid", String.valueOf(later)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].mid").value(String.valueOf(target)))
                .andExpect(jsonPath("$.items[0].uid").value("1001"))
                .andExpect(jsonPath("$.items[0].data.text").value("filter target"));
    }

    /**
     * 验证历史消息分页会返回 next_cursor，并能继续读取下一页。
     */
    @Test
    @DisplayName("history cursor paginates messages")
    void getChannelMessages_withLimitAndCursor_returnsNextPage() throws Exception {
        Fixture fixture = new Fixture();
        long first = fixture.sendText(1001L, "page first");
        long second = fixture.sendText(1001L, "page second");
        long third = fixture.sendText(1001L, "page third");

        MvcResult firstPage = fixture.mvc(1002L).perform(get("/api/channels/1/messages")
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].mid").value(String.valueOf(third)))
                .andExpect(jsonPath("$.items[1].mid").value(String.valueOf(second)))
                .andExpect(jsonPath("$.has_more").value(true))
                .andReturn();
        String nextCursor = fixture.readJson(firstPage).path("next_cursor").asText();

        fixture.mvc(1002L).perform(get("/api/channels/1/messages")
                        .param("cursor", nextCursor)
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].mid").value(String.valueOf(first)))
                .andExpect(jsonPath("$.has_more").value(false));
    }

    /**
     * 验证缺失认证上下文时，消息 HTTP 入口返回 unauthorized。
     */
    @Test
    @DisplayName("message endpoint without principal returns unauthorized")
    void messageEndpoint_withoutPrincipal_returnsUnauthorized() throws Exception {
        Fixture fixture = new Fixture();

        fixture.unauthenticatedMvc.perform(get("/api/channels/1/messages"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.reason").value("unauthorized"))
                .andExpect(jsonPath("$.error.message").value("authentication is required"));
    }

    /**
     * 验证 mention 已读接口会拒绝不存在的 ID，批量已读 before 上界只影响目标范围。
     */
    @Test
    @DisplayName("mention read failures and before boundary are enforced")
    void mentionRead_missingIdAndBeforeBoundary_returnsExpectedState() throws Exception {
        Fixture fixture = new Fixture();
        fixture.sendMention(1001L, 1002L, "read one");
        fixture.sendMention(1001L, 1002L, "read two");
        fixture.sendMention(1001L, 1002L, "read three");

        fixture.mvc(1002L).perform(put("/api/mentions/999999/read"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.reason").value("not_found"));

        fixture.mvc(1002L).perform(put("/api/mentions/read_state")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"before_mention_id":"7006","cid":"1"}
                                """))
                .andExpect(status().isNoContent());

        assertTrue(fixture.mentionRepository.mentions.stream()
                .filter(mention -> mention.mentionId() < 7006L)
                .allMatch(Mention::read));
        assertFalse(fixture.mentionRepository.mentions.stream()
                .filter(mention -> mention.mentionId() == 7006L)
                .findFirst()
                .orElseThrow()
                .read());
    }

    /**
     * 验证附件上传会拒绝非法附件类型和空文件。
     */
    @Test
    @DisplayName("attachment upload validates type and file size")
    void uploadMessageAttachment_invalidTypeOrEmptyFile_returnsValidationErrors() throws Exception {
        Fixture fixture = new Fixture();

        fixture.mvc(1001L).perform(multipart("/api/channels/1/messages/attachments")
                        .file(new MockMultipartFile("file", "image.png", "image/png", "content".getBytes()))
                        .param("message_type", "image"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.message").value("message_type must be file or voice"));

        fixture.mvc(1001L).perform(multipart("/api/channels/1/messages/attachments")
                        .file(new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]))
                        .param("message_type", "file"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.message").value("size must be greater than 0"));
    }

    /**
     * 验证转发会拒绝不存在的源消息和当前账号未加入的目标频道。
     */
    @Test
    @DisplayName("forward message rejects missing source and non member target channel")
    void forwardMessage_invalidSourceOrTarget_returnsExpectedErrors() throws Exception {
        Fixture fixture = new Fixture();

        fixture.mvc(1001L).perform(post("/api/messages/999999/forward")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"target_cid":"1","comment":"missing"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.reason").value("not_found"));

        long sourceMessageId = fixture.sendText(1001L, "source for blocked forward");
        fixture.mvc(1002L).perform(post("/api/messages/{messageId}/forward", sourceMessageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"target_cid":"2","comment":"blocked target"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.reason").value("not_channel_member"));
    }

    /**
     * 验证取消不存在的置顶会返回 not found，不会修改置顶集合。
     */
    @Test
    @DisplayName("unpin missing pin returns not found")
    void unpinChannelMessage_missingPin_returnsNotFound() throws Exception {
        Fixture fixture = new Fixture();
        long messageId = fixture.sendText(1001L, "not pinned");

        fixture.mvc(1001L).perform(delete("/api/channels/1/pins/{messageId}", messageId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.reason").value("not_found"));

        assertTrue(fixture.channelPinRepository.pins.isEmpty());
    }

    /**
     * 消息业务链路测试 fixture。
     * 职责：组装真实 controller 与领域 API，并以内存替身提供频道、消息、存储、时间和 ID 边界。
     */
    private static final class Fixture {

        private static final String SERVER_ID = "550e8400-e29b-41d4-a716-446655440000";

        final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        final JsonProvider jsonProvider = new JsonProvider(objectMapper);
        final RequestAuthenticationContext authRequestContext = new RequestAuthenticationContext();
        final IncrementingIdGenerator idGenerator = new IncrementingIdGenerator(7001L);
        final InMemoryChannelRepository channelRepository = new InMemoryChannelRepository();
        final InMemoryChannelMemberRepository channelMemberRepository = new InMemoryChannelMemberRepository();
        final InMemoryChannelAuditLogRepository channelAuditLogRepository = new InMemoryChannelAuditLogRepository();
        final InMemoryChannelPinRepository channelPinRepository = new InMemoryChannelPinRepository();
        final InMemoryMessageRepository messageRepository = new InMemoryMessageRepository();
        final InMemoryMentionRepository mentionRepository = new InMemoryMentionRepository();
        final InMemoryUserProfileRepository userProfileRepository = new InMemoryUserProfileRepository();
        final TestObjectStorageService storageService = new TestObjectStorageService();
        final RecordingMessageRealtimePublisher publisher = new RecordingMessageRealtimePublisher();
        final TimeProvider timeProvider = new TimeProvider(Clock.fixed(BASE_TIME, ZoneOffset.UTC));
        final TransactionRunner transactionRunner = new NoopTransactionRunner();

        private final MockMvc account1001Mvc;
        private final MockMvc account1002Mvc;
        private final MockMvc account1003Mvc;
        final MockMvc unauthenticatedMvc;

        Fixture() {
            channelRepository.channels.put(1L, new Channel(1L, 101L, "private-room", "", "", "1001", "private", true, BASE_TIME, BASE_TIME));
            channelRepository.channels.put(2L, new Channel(2L, 102L, "owner-room", "", "", "1001", "private", false, BASE_TIME, BASE_TIME));
            channelMemberRepository.save(new ChannelMember(1L, 1001L, ChannelMemberRole.OWNER, BASE_TIME, null));
            channelMemberRepository.save(new ChannelMember(1L, 1002L, ChannelMemberRole.MEMBER, BASE_TIME, null));
            channelMemberRepository.save(new ChannelMember(2L, 1001L, ChannelMemberRole.OWNER, BASE_TIME, null));
            ObjectProvider<ObjectStorageService> storageProvider = objectProvider(storageService);
            MessageAttachmentObjectKeyPolicy objectKeyPolicy = new MessageAttachmentObjectKeyPolicy();
            MessageAttachmentPayloadResolver payloadResolver = new MessageAttachmentPayloadResolver(storageProvider, jsonProvider);
            ChannelMessagePluginRegistry pluginRegistry = pluginRegistry(storageService, jsonProvider, objectKeyPolicy);
            MessageChannelBoundary channelBoundary = new ChannelBackedMessageChannelBoundary(
                    channelRepository,
                    channelMemberRepository,
                    channelAuditLogRepository,
                    channelPinRepository,
                    new ChannelGovernancePolicy()
            );
            ChannelMessagePublishingDomainApi publishingApi = new ChannelMessagePublishingDomainApi(
                    channelBoundary,
                    messageRepository,
                    mentionRepository,
                    userProfileRepository,
                    publisher,
                    pluginRegistry,
                    payloadResolver,
                    new ServerIdentityProperties(SERVER_ID),
                    idGenerator,
                    jsonProvider,
                    timeProvider,
                    transactionRunner
            );
            ChannelMessageTimelineDomainApi timelineApi = new ChannelMessageTimelineDomainApi(
                    channelBoundary,
                    messageRepository,
                    mentionRepository,
                    userProfileRepository,
                    publisher,
                    pluginRegistry,
                    payloadResolver,
                    new ServerIdentityProperties(SERVER_ID),
                    idGenerator,
                    jsonProvider,
                    timeProvider,
                    transactionRunner
            );
            ChannelMessageLifecycleDomainApi lifecycleApi = new ChannelMessageLifecycleDomainApi(
                    channelBoundary,
                    messageRepository,
                    mentionRepository,
                    userProfileRepository,
                    publisher,
                    pluginRegistry,
                    payloadResolver,
                    new ServerIdentityProperties(SERVER_ID),
                    idGenerator,
                    jsonProvider,
                    timeProvider,
                    transactionRunner
            );
            ChannelMessageAttachmentDomainApi attachmentApi = new ChannelMessageAttachmentDomainApi(
                    channelBoundary,
                    objectKeyPolicy,
                    idGenerator,
                    timeProvider,
                    storageProvider
            );
            ChannelPinDomainApi pinApi = new ChannelPinDomainApi(
                    channelBoundary,
                    messageRepository,
                    mentionRepository,
                    userProfileRepository,
                    publisher,
                    pluginRegistry,
                    payloadResolver,
                    new ServerIdentityProperties(SERVER_ID),
                    idGenerator,
                    jsonProvider,
                    timeProvider,
                    transactionRunner
            );
            MentionDomainApi mentionApi = new MentionDomainApi(mentionRepository);
            ChannelMessageV1ResponseMapper responseMapper = new ChannelMessageV1ResponseMapper(
                    new FixtureUserProfileApi(userProfileRepository),
                    jsonProvider
            );
            ChannelMessageController channelMessageController = new ChannelMessageController(
                    publishingApi,
                    timelineApi,
                    attachmentApi,
                    lifecycleApi,
                    authRequestContext,
                    responseMapper
            );
            MessageController messageController = new MessageController(
                    publishingApi,
                    lifecycleApi,
                    authRequestContext,
                    responseMapper
            );
            ChannelPinsController pinsController = new ChannelPinsController(pinApi, authRequestContext);
            MentionController mentionController = new MentionController(mentionApi, authRequestContext);
            this.account1001Mvc = mockMvc(1001L, channelMessageController, messageController, pinsController, mentionController);
            this.account1002Mvc = mockMvc(1002L, channelMessageController, messageController, pinsController, mentionController);
            this.account1003Mvc = mockMvc(1003L, channelMessageController, messageController, pinsController, mentionController);
            this.unauthenticatedMvc = unauthenticatedMockMvc(channelMessageController, messageController, pinsController, mentionController);
        }

        MockMvc mvc(long accountId) {
            return switch ((int) accountId) {
                case 1001 -> account1001Mvc;
                case 1002 -> account1002Mvc;
                case 1003 -> account1003Mvc;
                default -> throw new IllegalArgumentException("unsupported fixture accountId: " + accountId);
            };
        }

        long sendText(long accountId, String text) throws Exception {
            MvcResult result = mvc(accountId).perform(post("/api/channels/1/messages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"domain":"Core:Text","domain_version":"1.0.0","data":{"text":"%s"}}
                                    """.formatted(text)))
                    .andExpect(status().isCreated())
                    .andReturn();
            return Long.parseLong(readJson(result).path("mid").asText());
        }

        long sendMention(long fromAccountId, long targetAccountId, String text) throws Exception {
            MvcResult result = mvc(fromAccountId).perform(post("/api/channels/1/messages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "domain":"Core:Text",
                                      "domain_version":"1.0.0",
                                      "data":{"text":"%s"},
                                      "mentions":[{"type":"user","uid":"%d"}]
                                    }
                                    """.formatted(text, targetAccountId)))
                    .andExpect(status().isCreated())
                    .andReturn();
            return Long.parseLong(readJson(result).path("mid").asText());
        }

        JsonNode readJson(MvcResult result) throws Exception {
            return objectMapper.readTree(result.getResponse().getContentAsByteArray());
        }

        private MockMvc mockMvc(long accountId, Object... controllers) {
            return MockMvcBuilders.standaloneSetup(controllers)
                    .addInterceptors(new BindPrincipalInterceptor(authRequestContext, accountId))
                    .setControllerAdvice(new GlobalExceptionHandler())
                    .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                    .build();
        }

        private MockMvc unauthenticatedMockMvc(Object... controllers) {
            return MockMvcBuilders.standaloneSetup(controllers)
                    .setControllerAdvice(new GlobalExceptionHandler())
                    .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                    .build();
        }
    }

    private static ChannelMessagePluginRegistry pluginRegistry(
            ObjectStorageService storageService,
            JsonProvider jsonProvider,
            MessageAttachmentObjectKeyPolicy objectKeyPolicy
    ) {
        List<ChannelMessagePluginRegistration> registrations = new ArrayList<>();
        registrations.add(registration("builtin-text-message", "text", new TextChannelMessagePlugin()));
        registrations.add(registration("builtin-file-message", "file", new FileChannelMessagePlugin(storageService, jsonProvider, objectKeyPolicy)));
        registrations.add(registration("builtin-voice-message", "voice", new VoiceChannelMessagePlugin(storageService, jsonProvider, objectKeyPolicy)));
        return new ChannelMessagePluginRegistry(registrations);
    }

    private static ChannelMessagePluginRegistration registration(String pluginKey, String messageType, ChannelMessagePlugin plugin) {
        return new ChannelMessagePluginRegistration(
                new ChannelMessagePluginDescriptor(
                        pluginKey,
                        messageType,
                        messageType,
                        "Built-in " + messageType + " channel message plugin",
                        true,
                        List.of("message.sent", "message.recalled"),
                        List.of("message:" + messageType + ":send"),
                        "always_available"
                ),
                plugin
        );
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

    private static final class BindPrincipalInterceptor implements HandlerInterceptor {

        private final RequestAuthenticationContext authRequestContext;
        private final long accountId;

        private BindPrincipalInterceptor(RequestAuthenticationContext authRequestContext, long accountId) {
            this.authRequestContext = authRequestContext;
            this.accountId = accountId;
        }

        @Override
        public boolean preHandle(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, Object handler) {
            authRequestContext.bind(request, new AuthenticatedAccount(accountId, "carry-user-" + accountId));
            return true;
        }
    }

    private static final class InMemoryChannelRepository implements ChannelRepository {

        final Map<Long, Channel> channels = new HashMap<>();

        @Override
        public Optional<Channel> findDefaultChannel() {
            return Optional.ofNullable(channels.get(1L));
        }

        @Override
        public Optional<Channel> findSystemChannel() {
            return channels.values().stream().filter(channel -> "system".equals(channel.type())).findFirst();
        }

        @Override
        public Optional<Channel> findById(long channelId) {
            return Optional.ofNullable(channels.get(channelId));
        }
    }

    private static final class InMemoryChannelMemberRepository implements ChannelMemberRepository {

        final Map<Long, List<ChannelMember>> memberships = new HashMap<>();

        @Override
        public boolean exists(long channelId, long accountId) {
            return findByChannelIdAndAccountId(channelId, accountId).isPresent();
        }

        @Override
        public void save(ChannelMember channelMember) {
            memberships.computeIfAbsent(channelMember.channelId(), ignored -> new ArrayList<>()).add(channelMember);
        }

        @Override
        public void update(ChannelMember channelMember) {
            List<ChannelMember> members = new ArrayList<>(memberships.getOrDefault(channelMember.channelId(), List.of()));
            members.removeIf(member -> member.accountId() == channelMember.accountId());
            members.add(channelMember);
            memberships.put(channelMember.channelId(), members);
        }

        @Override
        public Optional<ChannelMember> findByChannelIdAndAccountId(long channelId, long accountId) {
            return memberships.getOrDefault(channelId, List.of()).stream()
                    .filter(member -> member.accountId() == accountId)
                    .findFirst();
        }

        @Override
        public List<Long> findAccountIdsByChannelId(long channelId) {
            return memberships.getOrDefault(channelId, List.of()).stream()
                    .map(ChannelMember::accountId)
                    .toList();
        }
    }

    private static final class InMemoryChannelPinRepository implements ChannelPinRepository {

        final List<ChannelPin> pins = new ArrayList<>();

        @Override
        public Optional<ChannelPin> findByChannelIdAndMessageId(long channelId, long messageId) {
            return pins.stream().filter(pin -> pin.channelId() == channelId && pin.messageId() == messageId).findFirst();
        }

        @Override
        public void save(ChannelPin channelPin) {
            pins.removeIf(pin -> pin.channelId() == channelPin.channelId() && pin.messageId() == channelPin.messageId());
            pins.add(channelPin);
        }

        @Override
        public void delete(long channelId, long messageId) {
            pins.removeIf(pin -> pin.channelId() == channelId && pin.messageId() == messageId);
        }

        @Override
        public List<ChannelPin> findByChannelIdBefore(long channelId, Long cursorMessageId, int limit) {
            return pins.stream()
                    .filter(pin -> pin.channelId() == channelId)
                    .filter(pin -> cursorMessageId == null || pin.messageId() < cursorMessageId)
                    .sorted(Comparator.comparingLong(ChannelPin::messageId).reversed())
                    .limit(limit)
                    .toList();
        }

        @Override
        public long countByChannelId(long channelId) {
            return pins.stream().filter(pin -> pin.channelId() == channelId).count();
        }
    }

    private static final class InMemoryChannelAuditLogRepository implements ChannelAuditLogRepository {

        final List<ChannelAuditLog> logs = new ArrayList<>();

        @Override
        public void append(ChannelAuditLog channelAuditLog) {
            logs.add(channelAuditLog);
        }
    }

    private static final class InMemoryMessageRepository implements MessageRepository {

        final Map<Long, ChannelMessage> messagesById = new HashMap<>();

        @Override
        public ChannelMessage save(ChannelMessage message) {
            messagesById.put(message.messageId(), message);
            return message;
        }

        @Override
        public Optional<ChannelMessage> findById(long messageId) {
            return Optional.ofNullable(messagesById.get(messageId));
        }

        @Override
        public ChannelMessage update(ChannelMessage message) {
            messagesById.put(message.messageId(), message);
            return message;
        }

        @Override
        public void delete(long messageId) {
            messagesById.remove(messageId);
        }

        @Override
        public List<ChannelMessage> findByChannelIdBefore(long channelId, Long cursorMessageId, int limit) {
            return messagesById.values().stream()
                    .filter(message -> message.channelId() == channelId)
                    .filter(message -> cursorMessageId == null || message.messageId() < cursorMessageId)
                    .sorted(Comparator.comparingLong(ChannelMessage::messageId).reversed())
                    .limit(limit)
                    .toList();
        }

        @Override
        public List<ChannelMessage> findByChannelIdAfter(long channelId, long afterMessageId, int limit) {
            return messagesById.values().stream()
                    .filter(message -> message.channelId() == channelId)
                    .filter(message -> message.messageId() > afterMessageId)
                    .sorted(Comparator.comparingLong(ChannelMessage::messageId))
                    .limit(limit)
                    .toList();
        }

        @Override
        public List<ChannelMessage> searchByChannelId(long channelId, String keyword, int limit) {
            return searchByChannelId(channelId, keyword, null, null, null, null, null, limit);
        }

        @Override
        public List<ChannelMessage> searchByChannelId(
                long channelId,
                String keyword,
                Long cursorMessageId,
                Long senderAccountId,
                String domain,
                Long beforeMessageId,
                Long afterMessageId,
                int limit
        ) {
            return messagesById.values().stream()
                    .filter(message -> message.channelId() == channelId)
                    .filter(message -> cursorMessageId == null || message.messageId() < cursorMessageId)
                    .filter(message -> senderAccountId == null || message.senderId() == senderAccountId)
                    .filter(message -> domain == null || domain.equals(message.messageType()))
                    .filter(message -> beforeMessageId == null || message.messageId() < beforeMessageId)
                    .filter(message -> afterMessageId == null || message.messageId() > afterMessageId)
                    .filter(message -> message.searchableText() != null && message.searchableText().contains(keyword.trim()))
                    .sorted(Comparator.comparingLong(ChannelMessage::messageId).reversed())
                    .limit(limit)
                    .toList();
        }
    }

    private static final class InMemoryMentionRepository implements MentionRepository {

        final List<Mention> mentions = new ArrayList<>();

        @Override
        public void save(Mention mention) {
            mentions.add(mention);
        }

        @Override
        public void deleteByMessageId(long messageId) {
            mentions.removeIf(mention -> mention.messageId() == messageId);
        }

        @Override
        public List<Mention> listByAccountId(long accountId, Long cursorMentionId, int limit, boolean unreadOnly, Long channelId) {
            return mentions.stream()
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
            for (int index = 0; index < mentions.size(); index++) {
                Mention mention = mentions.get(index);
                if (mention.mentionId() == mentionId && mention.targetAccountId() == accountId) {
                    mentions.set(index, new Mention(
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
            int updated = 0;
            for (int index = 0; index < mentions.size(); index++) {
                Mention mention = mentions.get(index);
                if (mention.targetAccountId() == accountId
                        && (beforeMentionId == null || mention.mentionId() < beforeMentionId)
                        && (channelId == null || mention.channelId() == channelId)
                        && !mention.read()) {
                    mentions.set(index, new Mention(
                            mention.mentionId(),
                            mention.channelId(),
                            mention.messageId(),
                            mention.fromAccountId(),
                            mention.targetType(),
                            mention.targetAccountId(),
                            mention.createdAt(),
                            true
                    ));
                    updated++;
                }
            }
            return updated;
        }
    }

    private static final class InMemoryUserProfileRepository implements UserProfileRepository {

        @Override
        public Optional<UserProfile> findByAccountId(long accountId) {
            return Optional.of(profile(accountId));
        }

        @Override
        public List<UserProfile> findAll() {
            return List.of();
        }

        @Override
        public List<UserProfile> findByAccountIdBefore(Long cursorAccountId, int limit) {
            return List.of();
        }

        @Override
        public List<UserProfile> searchByKeyword(String keyword, Long cursorAccountId, int limit) {
            return List.of();
        }

        @Override
        public UserProfile save(UserProfile userProfile) {
            return userProfile;
        }

        @Override
        public UserProfile update(UserProfile userProfile) {
            return userProfile;
        }

        private UserProfile profile(long accountId) {
            return new UserProfile(
                    accountId,
                    "carry-user-" + accountId,
                    "avatars/u/" + accountId + ".png",
                    "",
                    0L,
                    0L,
                    BASE_TIME,
                    BASE_TIME
            );
        }
    }

    private static final class FixtureUserProfileApi implements UserProfileApi {

        private final UserProfileRepository userProfileRepository;

        private FixtureUserProfileApi(UserProfileRepository userProfileRepository) {
            this.userProfileRepository = userProfileRepository;
        }

        @Override
        public UserProfileResult getCurrentUserProfile(GetCurrentUserProfileCommand command) {
            return result(command.accountId());
        }

        @Override
        public String getCurrentUserEmail(long accountId) {
            return "carry-user-" + accountId + "@example.com";
        }

        @Override
        public UserProfileResult getUserProfileByAccountId(GetUserProfileByAccountIdCommand command) {
            return result(command.accountId());
        }

        @Override
        public List<UserProfileResult> listUserProfiles(long accountId) {
            return List.of(result(accountId));
        }

        @Override
        public List<UserProfileResult> getPublicUserProfiles(List<Long> accountIds) {
            return accountIds.stream().map(this::result).toList();
        }

        @Override
        public UserProfilePageResult getUserProfiles(GetUserProfilesQuery query) {
            return new UserProfilePageResult(List.of(), null);
        }

        @Override
        public UserProfilePageResult searchUserProfiles(SearchUserProfilesQuery query) {
            return new UserProfilePageResult(List.of(), null);
        }

        @Override
        public UserProfileResult updateCurrentUserProfile(UpdateCurrentUserProfileCommand command) {
            return result(command.accountId());
        }

        @Override
        public void updateCurrentUserEmail(UpdateCurrentUserEmailCommand command) {
        }

        private UserProfileResult result(long accountId) {
            UserProfile profile = userProfileRepository.findByAccountId(accountId).orElseThrow();
            return new UserProfileResult(
                    profile.accountId(),
                    profile.nickname(),
                    profile.avatarUrl(),
                    profile.bio(),
                    profile.sex(),
                    profile.birthday(),
                    profile.createdAt(),
                    profile.updatedAt()
            );
        }
    }

    private static final class RecordingMessageRealtimePublisher implements MessageRealtimePublisher {

        final List<ChannelMessage> publishedMessages = new ArrayList<>();
        final List<ChannelMessage> updatedMessages = new ArrayList<>();
        final List<ChannelMessage> deletedMessages = new ArrayList<>();
        final List<List<Long>> recipientAccountIds = new ArrayList<>();
        final List<MessageChannelBoundary.MessageChannelPin> pinnedMessages = new ArrayList<>();
        final List<MessageChannelBoundary.MessageChannelPin> unpinnedMessages = new ArrayList<>();
        final List<Mention> createdMentions = new ArrayList<>();

        @Override
        public void publish(ChannelMessage message, MessageSenderSnapshot senderSnapshot, java.util.Collection<Long> recipients) {
            publishedMessages.add(message);
            recipientAccountIds.add(List.copyOf(recipients));
        }

        @Override
        public void publishUpdate(ChannelMessage message, MessageSenderSnapshot senderSnapshot, java.util.Collection<Long> recipientAccountIds) {
            updatedMessages.add(message);
        }

        @Override
        public void publishDelete(ChannelMessage message, java.util.Collection<Long> recipientAccountIds) {
            deletedMessages.add(message);
        }

        @Override
        public void publishPin(MessageChannelBoundary.MessageChannelPin pin, java.util.Collection<Long> recipientAccountIds) {
            pinnedMessages.add(pin);
        }

        @Override
        public void publishUnpin(MessageChannelBoundary.MessageChannelPin pin, long unpinnedByAccountId, long unpinnedAt, java.util.Collection<Long> recipientAccountIds) {
            unpinnedMessages.add(pin);
        }

        @Override
        public void publishMentionCreated(Mention mention, java.util.Collection<Long> recipientAccountIds) {
            createdMentions.add(mention);
        }
    }

    private static final class IncrementingIdGenerator implements IdGenerator {

        private long nextId;

        private IncrementingIdGenerator(long nextId) {
            this.nextId = nextId;
        }

        @Override
        public long nextLongId() {
            return nextId++;
        }
    }

    private static final class NoopTransactionRunner implements TransactionRunner {

        @Override
        public <T> T runInTransaction(java.util.function.Supplier<T> action) {
            return action.get();
        }

        @Override
        public void runInTransaction(Runnable action) {
            action.run();
        }
    }

    private static final class TestObjectStorageService implements ObjectStorageService {

        final Map<String, StorageObject> objects = new HashMap<>();
        PutObjectCommand lastPutCommand;

        @Override
        public StorageObject put(PutObjectCommand command) {
            this.lastPutCommand = command;
            StorageObject object = StorageObject.metadata(command.objectKey(), command.contentType(), command.size());
            objects.put(command.objectKey(), object);
            return object;
        }

        @Override
        public Optional<StorageObject> get(GetObjectCommand command) {
            return Optional.ofNullable(objects.get(command.objectKey()));
        }

        @Override
        public void delete(DeleteObjectCommand command) {
            objects.remove(command.objectKey());
        }

        @Override
        public PresignedUrl createPresignedUrl(PresignedUrlCommand command) {
            return new PresignedUrl(URI.create("http://127.0.0.1:9000/" + command.objectKey()), BASE_TIME.plusSeconds(1800));
        }
    }
}
