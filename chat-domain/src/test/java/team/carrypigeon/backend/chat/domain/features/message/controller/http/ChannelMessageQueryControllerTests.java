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
import team.carrypigeon.backend.chat.domain.shared.application.auth.AuthenticatedAccount;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelMessageHistoryResult;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelMessageSearchResult;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.MessageAttachmentUploadResult;
import team.carrypigeon.backend.chat.domain.features.message.application.service.MessageDeliveryApplicationService;
import team.carrypigeon.backend.chat.domain.features.message.application.service.MessageModerationApplicationService;
import team.carrypigeon.backend.chat.domain.features.message.application.service.MessageQueryApplicationService;
import team.carrypigeon.backend.chat.domain.features.user.application.dto.UserProfileResult;
import team.carrypigeon.backend.chat.domain.features.user.application.service.UserProfileApplicationService;
import team.carrypigeon.backend.chat.domain.shared.controller.OpaqueCursorCodec;
import team.carrypigeon.backend.chat.domain.shared.controller.advice.GlobalExceptionHandler;

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

    private MessageDeliveryApplicationService messageDeliveryApplicationService;
    private MessageModerationApplicationService messageModerationApplicationService;
    private MessageQueryApplicationService messageQueryApplicationService;
    private UserProfileApplicationService userProfileApplicationService;
    private RequestAuthenticationContext authRequestContext;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        messageDeliveryApplicationService = mock(MessageDeliveryApplicationService.class);
        messageModerationApplicationService = mock(MessageModerationApplicationService.class);
        messageQueryApplicationService = mock(MessageQueryApplicationService.class);
        userProfileApplicationService = mock(UserProfileApplicationService.class);
        authRequestContext = new RequestAuthenticationContext();
        mockMvc = MockMvcBuilders.standaloneSetup(
                        channelMessageController(),
                        new MessageController(messageModerationApplicationService, authRequestContext)
                )
                .setMessageConverters(snakeCaseConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("get channel messages returns v1 cursor page")
    void getChannelMessages_returnsV1CursorPage() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(messageQueryApplicationService.getChannelMessageHistory(any())).thenReturn(new ChannelMessageHistoryResult(
                List.of(messageResult(5001L, 1001L, "text", "hello world", "hello world", null, "sent")),
                5001L
        ));
        when(userProfileApplicationService.getUserProfileByAccountId(any())).thenReturn(senderProfile(1001L));

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

    @Test
    @DisplayName("search channel messages returns v1 cursor page")
    void searchChannelMessages_returnsV1CursorPage() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(messageQueryApplicationService.searchChannelMessages(any())).thenReturn(new ChannelMessageSearchResult(
                List.of(messageResult(5002L, 1001L, "text", "search body", "search body", null, "sent"))
        ));
        when(userProfileApplicationService.getUserProfileByAccountId(any())).thenReturn(senderProfile(1001L));

        mockMvc.perform(get("/api/channels/1/messages/search?q=search&limit=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].mid").value("5002"))
                .andExpect(jsonPath("$.items[0].preview").value("search body"))
                .andExpect(jsonPath("$.has_more").value(false));
    }

    @Test
    @DisplayName("search channel messages advanced filters are accepted")
    void searchChannelMessages_advancedFilters_areAccepted() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(messageQueryApplicationService.searchChannelMessages(any())).thenReturn(new ChannelMessageSearchResult(
                List.of(messageResult(5004L, 1002L, "text", "search body", "search body", null, "sent"))
        ));
        when(userProfileApplicationService.getUserProfileByAccountId(any())).thenReturn(senderProfile(1002L));

        mockMvc.perform(get("/api/channels/1/messages/search?q=search&sender_uid=1002&domain=Core:Text&before_mid=6000&after_mid=4000")
                        .param("cursor", OpaqueCursorCodec.encode(SEARCH_CURSOR_SCOPE, 7000L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].mid").value("5004"));

        verify(messageQueryApplicationService).searchChannelMessages(any());
    }

    @Test
    @DisplayName("get channel messages around_mid request returns contextual page")
    void getChannelMessages_aroundMid_returnsContextualPage() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(messageQueryApplicationService.getChannelMessageHistory(any())).thenReturn(new ChannelMessageHistoryResult(
                List.of(
                        messageResult(5003L, 1002L, "text", "third", "third", null, "sent"),
                        messageResult(5002L, 1001L, "text", "second", "second", null, "sent"),
                        messageResult(5001L, 1001L, "text", "first", "first", null, "sent")
                ),
                null
        ));
        when(userProfileApplicationService.getUserProfileByAccountId(any())).thenReturn(senderProfile(1001L));

        mockMvc.perform(get("/api/channels/1/messages?around_mid=5001&before=10&after=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].mid").value("5003"))
                .andExpect(jsonPath("$.has_more").value(false));
    }

    @Test
    @DisplayName("send channel message returns 201 and v1 payload")
    void sendChannelMessage_returns201AndV1Payload() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(messageDeliveryApplicationService.sendChannelMessageHttp(any())).thenReturn(
                messageResult(5003L, 1001L, "text", "hello", "hello", null, "sent")
        );
        when(userProfileApplicationService.getUserProfileByAccountId(any())).thenReturn(senderProfile(1001L));

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

    @Test
    @DisplayName("send file channel message returns structured attachment payload")
    void sendFileChannelMessage_returnsStructuredAttachmentPayload() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(messageDeliveryApplicationService.sendChannelMessageHttp(any())).thenReturn(
                messageResult(
                        5004L,
                        1001L,
                        "file",
                        "项目文档",
                        "[文件消息] demo.pdf",
                        "{\"share_key\":\"shr_7001\",\"download_path\":\"api/files/download/shr_7001\",\"filename\":\"demo.pdf\",\"mime_type\":\"application/pdf\",\"size\":123}",
                        "sent"
                )
        );
        when(userProfileApplicationService.getUserProfileByAccountId(any())).thenReturn(senderProfile(1001L));

        mockMvc.perform(post("/api/channels/1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"domain":"Core:File","domain_version":"1.0.0","data":{"share_key":"shr_7001","filename":"demo.pdf","mime_type":"application/pdf","size":123}}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mid").value("5004"))
                .andExpect(jsonPath("$.domain").value("Core:File"))
                .andExpect(jsonPath("$.data.share_key").value("shr_7001"))
                .andExpect(jsonPath("$.data.download_path").value("api/files/download/shr_7001"));
    }

    @Test
    @DisplayName("send file channel message object key request returns structured attachment payload")
    void sendFileChannelMessage_objectKeyRequest_returnsStructuredAttachmentPayload() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(messageDeliveryApplicationService.sendChannelMessageHttp(any())).thenReturn(
                messageResult(
                        5004L,
                        1001L,
                        "file",
                        "项目文档",
                        "[文件消息] demo.pdf",
                        "{\"share_key\":\"shr_att_demo\",\"download_path\":\"api/files/download/shr_att_demo\",\"filename\":\"demo.pdf\",\"mime_type\":\"application/pdf\",\"size\":123}",
                        "sent"
                )
        );
        when(userProfileApplicationService.getUserProfileByAccountId(any())).thenReturn(senderProfile(1001L));

        mockMvc.perform(post("/api/channels/1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"domain":"Core:File","domain_version":"1.0.0","data":{"object_key":"channels/1/messages/file/accounts/1001/5001-demo.pdf","filename":"demo.pdf","mime_type":"application/pdf","size":123}}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.share_key").value("shr_att_demo"));
    }

    @Test
    @DisplayName("upload message attachment returns transitional success envelope")
    void uploadMessageAttachment_returnsTransitionalSuccessEnvelope() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(messageDeliveryApplicationService.uploadMessageAttachment(anyLong(), anyLong(), any(), any(), any(), anyLong(), any()))
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

    @Test
    @DisplayName("delete message returns 204")
    void deleteMessage_returns204() throws Exception {
        mockMvc = authenticatedMockMvc();
        doNothing().when(messageModerationApplicationService).deleteChannelMessage(any());

        mockMvc.perform(delete("/api/messages/5009"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("edit message returns v1 payload with edited fields")
    void editMessage_returnsV1PayloadWithEditedFields() throws Exception {
        mockMvc = authenticatedMockMvc();
        when(messageModerationApplicationService.editChannelMessage(any())).thenReturn(new ChannelMessageResult(
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

    private MockMvc authenticatedMockMvc() {
        return MockMvcBuilders.standaloneSetup(
                        channelMessageController(),
                        new MessageController(messageModerationApplicationService, authRequestContext)
                )
                .addInterceptors(new BindPrincipalInterceptor(authRequestContext))
                .setMessageConverters(snakeCaseConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private ChannelMessageController channelMessageController() {
        return new ChannelMessageController(
                messageDeliveryApplicationService,
                messageModerationApplicationService,
                messageQueryApplicationService,
                userProfileApplicationService,
                authRequestContext
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
        return new UserProfileResult(accountId, "carry-user", "avatars/u/1001.png", "hello", Instant.parse("2026-04-20T12:00:00Z"), Instant.parse("2026-04-21T12:00:00Z"));
    }

    private MappingJackson2HttpMessageConverter snakeCaseConverter() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return new MappingJackson2HttpMessageConverter(objectMapper);
    }

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
