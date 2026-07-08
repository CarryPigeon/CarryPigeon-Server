package team.carrypigeon.backend.chat.domain.features.message.controller.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.time.Instant;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.HandlerInterceptor;
import team.carrypigeon.backend.chat.domain.shared.controller.support.RequestAuthenticationContext;
import team.carrypigeon.backend.chat.domain.shared.domain.auth.AuthenticatedAccount;
import team.carrypigeon.backend.chat.domain.features.message.controller.support.ChannelMessageV1ResponseMapper;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelMessageHistoryResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelMessageSearchResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.MessageAttachmentUploadResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.ChannelMessageAttachmentApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.ChannelMessageLifecycleApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.ChannelMessagePublishingApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.ChannelMessageTimelineApi;
import team.carrypigeon.backend.chat.domain.features.user.domain.projection.UserProfileResult;
import team.carrypigeon.backend.chat.domain.features.user.domain.api.UserProfileApi;
import team.carrypigeon.backend.chat.domain.shared.controller.OpaqueCursorCodec;
import team.carrypigeon.backend.chat.domain.shared.controller.advice.GlobalExceptionHandler;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ChannelMessageController 查询协议测试。
 * 职责：验证 v1 消息历史、搜索、发送与删除入口的协议契约。
 * 边界：不验证真实数据库访问与上传链路，只验证 HTTP 协议层行为。
 */
@Tag("contract")
class ChannelMessageQueryControllerTests {

    private static final String HISTORY_CURSOR_SCOPE = "channel_messages";
    private static final String SEARCH_CURSOR_SCOPE = "channel_message_search";

    private ChannelMessagePublishingApi channelMessagePublishingApi;
    private ChannelMessageTimelineApi channelMessageTimelineApi;
    private ChannelMessageLifecycleApi channelMessageLifecycleApi;
    private ChannelMessageAttachmentApi channelMessageAttachmentDomainApi;
    private UserProfileApi userProfileDomainApi;
    private RequestAuthenticationContext authRequestContext;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        channelMessagePublishingApi = mock(ChannelMessagePublishingApi.class);
        channelMessageTimelineApi = mock(ChannelMessageTimelineApi.class);
        channelMessageLifecycleApi = mock(ChannelMessageLifecycleApi.class);
        channelMessageAttachmentDomainApi = mock(ChannelMessageAttachmentApi.class);
        userProfileDomainApi = mock(UserProfileApi.class);
        authRequestContext = new RequestAuthenticationContext();
        mockMvc = MockMvcBuilders.standaloneSetup(
                        channelMessageController(),
                        new MessageController(channelMessagePublishingApi, channelMessageLifecycleApi, authRequestContext, responseMapper())
                )
                .setMessageConverters(snakeCaseConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * 验证 `getChannelMessages` 在 `returnsV1CursorPage` 场景下的测试契约。
     */
    @Test
    @DisplayName("get channel messages returns v1 cursor page")
    void getChannelMessages_returnsV1CursorPage() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelMessageTimelineApi.getChannelMessageHistory(any())).thenReturn(new ChannelMessageHistoryResult(
                List.of(messageResult(5001L, 1001L, "text", "hello world", "hello world", null, "sent")),
                5001L
        ));
        when(userProfileDomainApi.getUserProfileByAccountId(any())).thenReturn(senderProfile(1001L));

        mockMvc.perform(get("/api/channels/1/messages?limit=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].mid").value("5001"))
                .andExpect(jsonPath("$.items[0].cid").value("1"))
                .andExpect(jsonPath("$.items[0].uid").value("1001"))
                .andExpect(jsonPath("$.items[0].sender.uid").value("1001"))
                .andExpect(jsonPath("$.items[0].domain").value("Core:Text"))
                .andExpect(jsonPath("$.items[0].data.text").value("hello world"))
                .andExpect(jsonPath("$.next_cursor").value(OpaqueCursorCodec.encode(HISTORY_CURSOR_SCOPE, 5001L)))
                .andExpect(jsonPath("$.has_more").value(true));
    }

    /**
     * 验证 `searchChannelMessages` 在 `returnsV1CursorPage` 场景下的测试契约。
     */
    @Test
    @DisplayName("search channel messages returns v1 cursor page")
    void searchChannelMessages_returnsV1CursorPage() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelMessageTimelineApi.searchChannelMessages(any())).thenReturn(new ChannelMessageSearchResult(
                List.of(messageResult(5002L, 1001L, "text", "search body", "search body", null, "sent"))
        ));
        when(userProfileDomainApi.getUserProfileByAccountId(any())).thenReturn(senderProfile(1001L));

        mockMvc.perform(get("/api/channels/1/messages/search?q=search&limit=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].mid").value("5002"))
                .andExpect(jsonPath("$.items[0].preview").value("search body"))
                .andExpect(jsonPath("$.has_more").value(false));
    }

    /**
     * 验证 `searchChannelMessages` 在 `advancedFilters` 条件下满足 `areAccepted` 的测试契约。
     */
    @Test
    @DisplayName("search channel messages advanced filters are accepted")
    void searchChannelMessages_advancedFilters_areAccepted() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelMessageTimelineApi.searchChannelMessages(any())).thenReturn(new ChannelMessageSearchResult(
                List.of(messageResult(5004L, 1002L, "text", "search body", "search body", null, "sent"))
        ));
        when(userProfileDomainApi.getUserProfileByAccountId(any())).thenReturn(senderProfile(1002L));

        mockMvc.perform(get("/api/channels/1/messages/search?q=search&sender_uid=1002&domain=Core:Text&before_mid=6000&after_mid=4000")
                        .param("cursor", OpaqueCursorCodec.encode(SEARCH_CURSOR_SCOPE, 7000L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].mid").value("5004"));

        verify(channelMessageTimelineApi).searchChannelMessages(any());
    }

    /**
     * 验证 `getChannelMessages` 在 `aroundMid` 条件下满足 `returnsContextualPage` 的测试契约。
     */
    @Test
    @DisplayName("get channel messages around_mid request returns contextual page")
    void getChannelMessages_aroundMid_returnsContextualPage() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelMessageTimelineApi.getChannelMessageHistory(any())).thenReturn(new ChannelMessageHistoryResult(
                List.of(
                        messageResult(5003L, 1002L, "text", "third", "third", null, "sent"),
                        messageResult(5002L, 1001L, "text", "second", "second", null, "sent"),
                        messageResult(5001L, 1001L, "text", "first", "first", null, "sent")
                ),
                null
        ));
        when(userProfileDomainApi.getUserProfileByAccountId(any())).thenReturn(senderProfile(1001L));

        mockMvc.perform(get("/api/channels/1/messages?around_mid=5001&before=10&after=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].mid").value("5003"))
                .andExpect(jsonPath("$.has_more").value(false));
    }

    /**
     * 验证 `sendChannelMessage` 在 `returns201AndV1Payload` 场景下的测试契约。
     */
    @Test
    @DisplayName("send channel message returns 201 and v1 payload")
    void sendChannelMessage_returns201AndV1Payload() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelMessagePublishingApi.sendChannelMessageHttp(any())).thenReturn(
                messageResult(5003L, 1001L, "text", "hello", "hello", null, "sent")
        );
        when(userProfileDomainApi.getUserProfileByAccountId(any())).thenReturn(senderProfile(1001L));

        mockMvc.perform(post("/api/channels/1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"domain":"Core:Text","domain_version":"1.0.0","data":{"text":"hello"},"reply_to_mid":"0"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mid").value("5003"))
                .andExpect(jsonPath("$.domain").value("Core:Text"))
                .andExpect(jsonPath("$.data.text").value("hello"));
    }

    /**
     * 验证发送消息请求体为 JSON null 时返回稳定校验错误。
     */
    @Test
    @DisplayName("send channel message null body returns validation error")
    void sendChannelMessage_nullBody_returnsValidationError() throws Exception {
        mockMvc = authenticatedMockMvc();

        mockMvc.perform(post("/api/channels/1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("null"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.reason").value("validation_failed"));
    }

    /**
     * 验证 `sendFileChannelMessage` 在 `returnsStructuredAttachmentPayload` 场景下的测试契约。
     */
    @Test
    @DisplayName("send file channel message returns structured attachment payload")
    void sendFileChannelMessage_returnsStructuredAttachmentPayload() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelMessagePublishingApi.sendChannelMessageHttp(any())).thenReturn(
                messageResult(
                        5004L,
                        1001L,
                        "file",
                        "项目文档",
                        "[文件消息] demo.pdf",
                        "{\"share_key\":\"shr_7001\",\"download_path\":\"/api/files/download/shr_7001\",\"filename\":\"demo.pdf\",\"mime_type\":\"application/pdf\",\"size\":123}",
                        "sent"
                )
        );
        when(userProfileDomainApi.getUserProfileByAccountId(any())).thenReturn(senderProfile(1001L));

        mockMvc.perform(post("/api/channels/1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"domain":"Core:File","domain_version":"1.0.0","data":{"share_key":"shr_7001","filename":"demo.pdf","mime_type":"application/pdf","size":123}}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mid").value("5004"))
                .andExpect(jsonPath("$.domain").value("Core:File"))
                .andExpect(jsonPath("$.data.share_key").value("shr_7001"))
                .andExpect(jsonPath("$.data.download_path").value("/api/files/download/shr_7001"));
    }

    /**
     * 验证 `sendFileChannelMessage` 在 `objectKeyRequest` 条件下满足 `returnsStructuredAttachmentPayload` 的测试契约。
     */
    @Test
    @DisplayName("send file channel message object key request returns structured attachment payload")
    void sendFileChannelMessage_objectKeyRequest_returnsStructuredAttachmentPayload() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelMessagePublishingApi.sendChannelMessageHttp(any())).thenReturn(
                messageResult(
                        5004L,
                        1001L,
                        "file",
                        "项目文档",
                        "[文件消息] demo.pdf",
                        "{\"share_key\":\"shr_att_demo\",\"download_path\":\"/api/files/download/shr_att_demo\",\"filename\":\"demo.pdf\",\"mime_type\":\"application/pdf\",\"size\":123}",
                        "sent"
                )
        );
        when(userProfileDomainApi.getUserProfileByAccountId(any())).thenReturn(senderProfile(1001L));

        mockMvc.perform(post("/api/channels/1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"domain":"Core:File","domain_version":"1.0.0","data":{"object_key":"channels/1/messages/file/accounts/1001/5001-demo.pdf","filename":"demo.pdf","mime_type":"application/pdf","size":123}}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.share_key").value("shr_att_demo"));
    }

    /**
     * 验证 `uploadMessageAttachment` 在 `returnsTransitionalSuccessEnvelope` 场景下的测试契约。
     */
    @Test
    @DisplayName("upload message attachment returns transitional success envelope")
    void uploadMessageAttachment_returnsTransitionalSuccessEnvelope() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelMessageAttachmentDomainApi.uploadMessageAttachment(anyLong(), anyLong(), any(), any(), any(), anyLong(), any()))
                .thenReturn(new MessageAttachmentUploadResult(
                        "channels/1/messages/file/accounts/1001/5001-demo.pdf",
                        "shr_att_demo",
                        "demo.pdf",
                        "application/pdf",
                        123L
                ));

        mockMvc.perform(multipart("/api/channels/1/messages/attachments")
                        .file(new MockMultipartFile("file", "demo.pdf", "application/pdf", "demo".getBytes()))
                        .param("message_type", "file"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(100))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.object_key").value("channels/1/messages/file/accounts/1001/5001-demo.pdf"))
                .andExpect(jsonPath("$.data.share_key").value("shr_att_demo"));
    }

    /**
     * 验证 `deleteMessage` 在 `returns204` 场景下的测试契约。
     */
    @Test
    @DisplayName("delete message returns 204")
    void deleteMessage_returns204() throws Exception {
        mockMvc = authenticatedMockMvc();
        doNothing().when(channelMessageLifecycleApi).deleteChannelMessage(any());

        mockMvc.perform(delete("/api/messages/5009"))
                .andExpect(status().isNoContent());
    }

    /**
     * 验证 `editMessage` 在 `returnsV1PayloadWithEditedFields` 场景下的测试契约。
     */
    @Test
    @DisplayName("edit message returns v1 payload with edited fields")
    void editMessage_returnsV1PayloadWithEditedFields() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelMessageLifecycleApi.editChannelMessage(any())).thenReturn(new ChannelMessageResult(
                5009L,
                "550e8400-e29b-41d4-a716-446655440000",
                1L,
                1L,
                1001L,
                "text",
                "edited content",
                "edited content",
                null,
                null,
                "[{\"type\":\"user\",\"uid\":\"1002\"}]",
                null,
                "sent",
                Instant.parse("2026-04-22T00:00:00Z"),
                Instant.parse("2026-04-22T00:05:00Z"),
                2L
        ));

        mockMvc.perform(patch("/api/messages/5009")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"domain":"Core:Text","domain_version":"1.0.0","data":{"text":"edited content"},"mentions":[{"type":"user","uid":"1002"}],"expected_edit_version":1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mid").value("5009"))
                .andExpect(jsonPath("$.data.text").value("edited content"))
                .andExpect(jsonPath("$.edit_version").value(2))
                .andExpect(jsonPath("$.mentions[0].uid").value("1002"));
    }

    /**
     * 验证编辑消息请求体字段缺失时返回统一校验错误，而不是进入领域服务。
     */
    @Test
    @DisplayName("edit message invalid body returns validation error")
    void editMessage_invalidBody_returnsValidationError() throws Exception {
        mockMvc = authenticatedMockMvc();

        mockMvc.perform(patch("/api/messages/5009")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.reason").value("validation_failed"));
    }

    /**
     * 验证转发消息请求体字段缺失时返回统一校验错误，而不是进入领域服务。
     */
    @Test
    @DisplayName("forward message invalid body returns validation error")
    void forwardMessage_invalidBody_returnsValidationError() throws Exception {
        mockMvc = authenticatedMockMvc();

        mockMvc.perform(post("/api/messages/5001/forward")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.reason").value("validation_failed"));
    }

    /**
     * 验证撤回消息 HTTP 入口会把频道、消息与当前账号映射到生命周期领域 API。
     */
    @Test
    @DisplayName("recall channel message returns v1 payload")
    void recallChannelMessage_returnsV1Payload() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(channelMessageLifecycleApi.recallChannelMessage(any())).thenReturn(
                messageResult(5010L, 1001L, "text", "[消息已撤回]", "[消息已撤回]", null, "recalled")
        );
        when(userProfileDomainApi.getUserProfileByAccountId(any())).thenReturn(senderProfile(1001L));

        mockMvc.perform(post("/api/channels/1/messages/5010/recall"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mid").value("5010"))
                .andExpect(jsonPath("$.data.text").value("[消息已撤回]"));

        verify(channelMessageLifecycleApi).recallChannelMessage(any());
    }

    private MockMvc authenticatedMockMvc() {
        return MockMvcBuilders.standaloneSetup(
                        channelMessageController(),
                        new MessageController(channelMessagePublishingApi, channelMessageLifecycleApi, authRequestContext, responseMapper())
                )
                .addInterceptors(new BindPrincipalInterceptor(authRequestContext))
                .setMessageConverters(snakeCaseConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private ChannelMessageController channelMessageController() {
        return new ChannelMessageController(
                channelMessagePublishingApi,
                channelMessageTimelineApi,
                channelMessageAttachmentDomainApi,
                channelMessageLifecycleApi,
                authRequestContext,
                responseMapper()
        );
    }

    private ChannelMessageResult messageResult(long messageId, long senderId, String messageType, String body, String preview, String payload, String status) {
        return new ChannelMessageResult(
                messageId,
                "550e8400-e29b-41d4-a716-446655440000",
                1L,
                1L,
                senderId,
                messageType,
                body,
                preview,
                payload,
                null,
                status,
                Instant.parse("2026-04-22T00:00:00Z")
        );
    }

    private UserProfileResult senderProfile(long accountId) {
        return new UserProfileResult(accountId, "carry-user", "avatars/u/1001.png", "hello", 0L, 0L, Instant.parse("2026-04-20T12:00:00Z"), Instant.parse("2026-04-21T12:00:00Z"));
    }

    private MappingJackson2HttpMessageConverter snakeCaseConverter() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return new MappingJackson2HttpMessageConverter(objectMapper);
    }

    private ChannelMessageV1ResponseMapper responseMapper() {
        return new ChannelMessageV1ResponseMapper(userProfileDomainApi, new JsonProvider(new ObjectMapper().findAndRegisterModules()));
    }

    /**
     * `BindPrincipalInterceptor` 测试辅助类型。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    private static class BindPrincipalInterceptor implements HandlerInterceptor {

        private final RequestAuthenticationContext authRequestContext;

        private BindPrincipalInterceptor(RequestAuthenticationContext authRequestContext) {
            this.authRequestContext = authRequestContext;
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            authRequestContext.bind(request, new AuthenticatedAccount(1001L, "carry-user@example.com"));
            return true;
        }
    }
}
