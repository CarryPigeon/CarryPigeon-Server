package team.carrypigeon.backend.chat.domain.controller.web.api.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;
import team.carrypigeon.backend.api.starter.server.ServerInfoConfig;
import team.carrypigeon.backend.chat.domain.controller.web.api.auth.AccessTokenService;
import team.carrypigeon.backend.chat.domain.controller.web.api.config.CpApiProperties;
import team.carrypigeon.backend.chat.domain.service.ws.ApiWsEventPublisher;
import team.carrypigeon.backend.chat.domain.service.ws.ApiWsEventStore;
import team.carrypigeon.backend.chat.domain.service.ws.ApiWsSessionRegistry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiWebSocketHandlerTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void handleTextMessage_authUnsupportedApiVersion_expectedApiVersionUnsupported() throws Exception {
        AccessTokenService accessTokenService = mock(AccessTokenService.class);
        ApiWebSocketHandler handler = newHandler(accessTokenService);
        WebSocketSession session = mockOpenSession();

        handler.handleTextMessage(session, new TextMessage("""
                {"type":"auth","id":"1","data":{"api_version":2,"access_token":"token"}}
                """));

        TextMessage sent = captureSingleMessage(session);
        JsonNode root = objectMapper.readTree(sent.getPayload());

        assertEquals("auth.err", root.path("type").asText());
        assertEquals("1", root.path("id").asText());
        assertEquals(CPProblemReason.API_VERSION_UNSUPPORTED.code(), root.path("error").path("reason").asText());

        verify(accessTokenService, never()).verifyInfo(any());
    }

    @Test
    void handleTextMessage_authMissingToken_expectedUnauthorized() throws Exception {
        AccessTokenService accessTokenService = mock(AccessTokenService.class);
        ApiWebSocketHandler handler = newHandler(accessTokenService);
        WebSocketSession session = mockOpenSession();

        handler.handleTextMessage(session, new TextMessage("""
                {"type":"auth","id":"1","data":{"api_version":1}}
                """));

        TextMessage sent = captureSingleMessage(session);
        JsonNode root = objectMapper.readTree(sent.getPayload());

        assertEquals("auth.err", root.path("type").asText());
        assertEquals("1", root.path("id").asText());
        assertEquals(CPProblemReason.UNAUTHORIZED.code(), root.path("error").path("reason").asText());

        verify(accessTokenService).verifyInfo(isNull());
    }

    @Test
    void handleTextMessage_authSuccess_expectedAuthOkPayloadAndRegister() throws Exception {
        AccessTokenService accessTokenService = mock(AccessTokenService.class);
        ServerInfoConfig serverInfoConfig = mock(ServerInfoConfig.class);
        ApiWsSessionRegistry sessionRegistry = mock(ApiWsSessionRegistry.class);
        when(serverInfoConfig.getServerId()).thenReturn("srv_1");
        when(accessTokenService.verifyInfo("valid_token")).thenReturn(new AccessTokenService.TokenInfo(123L, 1700000000000L));

        ApiWebSocketHandler handler = new ApiWebSocketHandler(
                objectMapper,
                new CpApiProperties(),
                accessTokenService,
                serverInfoConfig,
                sessionRegistry,
                mock(ApiWsEventStore.class),
                mock(ApiWsEventPublisher.class)
        );
        WebSocketSession session = mockOpenSession();

        handler.handleTextMessage(session, new TextMessage("""
                {"type":"auth","id":"1","data":{"api_version":1,"access_token":"valid_token"}}
                """));

        TextMessage sent = captureSingleMessage(session);
        JsonNode root = objectMapper.readTree(sent.getPayload());

        assertEquals("auth.ok", root.path("type").asText());
        assertEquals("1", root.path("id").asText());
        assertEquals("123", root.path("data").path("uid").asText());
        assertEquals(1700000000000L, root.path("data").path("expires_at").asLong());
        assertEquals("srv_1", root.path("data").path("server_id").asText());

        verify(sessionRegistry).register(123L, session);
    }

    @Test
    void handleTextMessage_authWithResumeFailed_expectedResumeFailedEventTooOld() throws Exception {
        AccessTokenService accessTokenService = mock(AccessTokenService.class);
        ServerInfoConfig serverInfoConfig = mock(ServerInfoConfig.class);
        ApiWsSessionRegistry sessionRegistry = mock(ApiWsSessionRegistry.class);
        ApiWsEventStore eventStore = mock(ApiWsEventStore.class);

        when(serverInfoConfig.getServerId()).thenReturn("srv_1");
        when(accessTokenService.verifyInfo("valid_token")).thenReturn(new AccessTokenService.TokenInfo(123L, 1700000000000L));
        when(eventStore.resumeAfter(eq("50"), any(Integer.class)))
                .thenReturn(new ApiWsEventStore.ResumeResult(List.of(), CPProblemReason.EVENT_TOO_OLD.code()));

        ApiWebSocketHandler handler = new ApiWebSocketHandler(
                objectMapper,
                new CpApiProperties(),
                accessTokenService,
                serverInfoConfig,
                sessionRegistry,
                eventStore,
                mock(ApiWsEventPublisher.class)
        );
        WebSocketSession session = mockOpenSession();

        handler.handleTextMessage(session, new TextMessage("""
                {"type":"auth","id":"1","data":{"api_version":1,"access_token":"valid_token","resume":{"last_event_id":"50"}}}
                """));

        org.mockito.ArgumentCaptor<TextMessage> captor = org.mockito.ArgumentCaptor.forClass(TextMessage.class);
        verify(session, times(2)).sendMessage(captor.capture());

        JsonNode first = objectMapper.readTree(captor.getAllValues().get(0).getPayload());
        JsonNode second = objectMapper.readTree(captor.getAllValues().get(1).getPayload());

        assertEquals("auth.ok", first.path("type").asText());
        assertEquals("resume.failed", second.path("type").asText());
        assertEquals(CPProblemReason.EVENT_TOO_OLD.code(), second.path("data").path("reason").asText());
    }

    @Test
    void handleTextMessage_reauthMissingData_expectedValidationFailedEnvelope() throws Exception {
        AccessTokenService accessTokenService = mock(AccessTokenService.class);
        ApiWebSocketHandler handler = newHandler(accessTokenService);
        WebSocketSession session = mockOpenSession();

        handler.handleTextMessage(session, new TextMessage("""
                {"type":"reauth","id":"9"}
                """));

        TextMessage sent = captureSingleMessage(session);
        JsonNode root = objectMapper.readTree(sent.getPayload());

        assertEquals("reauth.err", root.path("type").asText());
        assertEquals("9", root.path("id").asText());
        assertEquals(CPProblemReason.VALIDATION_FAILED.code(), root.path("error").path("reason").asText());
        assertEquals("validation failed", root.path("error").path("message").asText());
    }

    @Test
    void handleTextMessage_subscribeUnauthorized_expectedUnauthorizedEnvelope() throws Exception {
        AccessTokenService accessTokenService = mock(AccessTokenService.class);
        ApiWebSocketHandler handler = newHandler(accessTokenService);
        WebSocketSession session = mockOpenSession();

        handler.handleTextMessage(session, new TextMessage("""
                {"type":"subscribe","id":"7","data":{"cids":["1"]}}
                """));

        TextMessage sent = captureSingleMessage(session);
        JsonNode root = objectMapper.readTree(sent.getPayload());

        assertEquals("subscribe.err", root.path("type").asText());
        assertEquals("7", root.path("id").asText());
        assertEquals(CPProblemReason.UNAUTHORIZED.code(), root.path("error").path("reason").asText());
        assertEquals("unauthorized", root.path("error").path("message").asText());
    }

    @Test
    void handleTextMessage_unknownType_expectedNoResponse() throws Exception {
        AccessTokenService accessTokenService = mock(AccessTokenService.class);
        ApiWebSocketHandler handler = newHandler(accessTokenService);
        WebSocketSession session = mockOpenSession();

        handler.handleTextMessage(session, new TextMessage("""
                {"type":"future.command","id":"x","data":{}}
                """));

        verify(session, never()).sendMessage(any());
    }

    /**
     * 测试辅助方法。
     *
     * @param accessTokenService 测试输入参数
     * @return 测试辅助方法返回结果
     */
    private ApiWebSocketHandler newHandler(AccessTokenService accessTokenService) {
        return new ApiWebSocketHandler(
                objectMapper,
                new CpApiProperties(),
                accessTokenService,
                mock(ServerInfoConfig.class),
                mock(ApiWsSessionRegistry.class),
                mock(ApiWsEventStore.class),
                mock(ApiWsEventPublisher.class)
        );
    }

    /**
     * 测试辅助方法。
     *
     * @return 测试辅助方法返回结果
     */
    private WebSocketSession mockOpenSession() {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("s1");
        when(session.isOpen()).thenReturn(true);
        Map<String, Object> attrs = new HashMap<>();
        when(session.getAttributes()).thenReturn(attrs);
        return session;
    }

    /**
     * 测试辅助方法。
     *
     * @param session 测试输入参数
     * @return 测试辅助方法返回结果
     * @throws Exception 执行过程中抛出的异常
     */
    private TextMessage captureSingleMessage(WebSocketSession session) throws Exception {
        org.mockito.ArgumentCaptor<TextMessage> captor = org.mockito.ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());
        return captor.getValue();
    }
}
