package team.carrypigeon.backend.chat.domain.service.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiWsEventPublisherTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void publishToUsers_subscribedCidMatched_expectedSend() throws Exception {
        ApiWsSessionRegistry sessionRegistry = mock(ApiWsSessionRegistry.class);
        ApiWsEventStore eventStore = mock(ApiWsEventStore.class);
        ApiWsEventPublisher publisher = new ApiWsEventPublisher(
                objectMapper,
                sessionRegistry,
                eventStore,
                mock(ApiWsPayloadMapper.class),
                mock(ChannelMemberDao.class)
        );

        WebSocketSession session = mockOpenSession(Map.of(ApiWsSessionAttributes.SUB_CIDS, Set.of(100L)));
        when(sessionRegistry.sessionsOf(1L)).thenReturn(List.of(session));

        publisher.publishToUsers(List.of(1L), "message.created", objectMapper.createObjectNode().put("cid", "100"));

        verify(eventStore).append(any(ApiWsEventStore.StoredEvent.class));
        TextMessage sent = captureSingleMessage(session);
        JsonNode root = objectMapper.readTree(sent.getPayload());
        assertEquals("event", root.path("type").asText());
        assertEquals("message.created", root.path("data").path("event_type").asText());
    }

    @Test
    void publishToUsers_subscribedCidNotMatched_expectedSkipSend() throws Exception {
        ApiWsSessionRegistry sessionRegistry = mock(ApiWsSessionRegistry.class);
        ApiWsEventStore eventStore = mock(ApiWsEventStore.class);
        ApiWsEventPublisher publisher = new ApiWsEventPublisher(
                objectMapper,
                sessionRegistry,
                eventStore,
                mock(ApiWsPayloadMapper.class),
                mock(ChannelMemberDao.class)
        );

        WebSocketSession session = mockOpenSession(Map.of(ApiWsSessionAttributes.SUB_CIDS, Set.of(200L)));
        when(sessionRegistry.sessionsOf(1L)).thenReturn(List.of(session));

        publisher.publishToUsers(List.of(1L), "message.created", objectMapper.createObjectNode().put("cid", "100"));

        verify(eventStore).append(any(ApiWsEventStore.StoredEvent.class));
        verify(session, never()).sendMessage(any());
    }

    @Test
    void publishToUsers_nonChannelEventWithSubscription_expectedSend() throws Exception {
        ApiWsSessionRegistry sessionRegistry = mock(ApiWsSessionRegistry.class);
        ApiWsEventStore eventStore = mock(ApiWsEventStore.class);
        ApiWsEventPublisher publisher = new ApiWsEventPublisher(
                objectMapper,
                sessionRegistry,
                eventStore,
                mock(ApiWsPayloadMapper.class),
                mock(ChannelMemberDao.class)
        );

        WebSocketSession session = mockOpenSession(Map.of(ApiWsSessionAttributes.SUB_CIDS, Set.of(200L)));
        when(sessionRegistry.sessionsOf(1L)).thenReturn(List.of(session));

        publisher.publishToUsers(List.of(1L), "channels.changed", objectMapper.createObjectNode().put("hint", "refresh"));

        verify(eventStore).append(any(ApiWsEventStore.StoredEvent.class));
        TextMessage sent = captureSingleMessage(session);
        JsonNode root = objectMapper.readTree(sent.getPayload());
        assertEquals("event", root.path("type").asText());
        assertEquals("channels.changed", root.path("data").path("event_type").asText());
    }

    @Test
    void publishToUsers_subscriptionAsStringCollection_expectedSend() throws Exception {
        ApiWsSessionRegistry sessionRegistry = mock(ApiWsSessionRegistry.class);
        ApiWsEventStore eventStore = mock(ApiWsEventStore.class);
        ApiWsEventPublisher publisher = new ApiWsEventPublisher(
                objectMapper,
                sessionRegistry,
                eventStore,
                mock(ApiWsPayloadMapper.class),
                mock(ChannelMemberDao.class)
        );

        WebSocketSession session = mockOpenSession(Map.of(ApiWsSessionAttributes.SUB_CIDS, List.of("100", "101")));
        when(sessionRegistry.sessionsOf(1L)).thenReturn(List.of(session));

        publisher.publishToUsers(List.of(1L), "message.created", objectMapper.createObjectNode().put("cid", 100L));

        verify(eventStore).append(any(ApiWsEventStore.StoredEvent.class));
        verify(session).sendMessage(any(TextMessage.class));
    }

    /**
     * 测试辅助方法。
     *
     * @param attrs 会话属性映射
     * @param attrs 测试输入参数
     * @return 测试辅助方法返回结果
     */
    private WebSocketSession mockOpenSession(Map<String, Object> attrs) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("s1");
        when(session.isOpen()).thenReturn(true);
        Map<String, Object> map = new HashMap<>(attrs);
        when(session.getAttributes()).thenReturn(map);
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
        TextMessage message = captor.getValue();
        assertTrue(message.getPayload().contains("\"type\":\"event\""));
        return message;
    }
}
