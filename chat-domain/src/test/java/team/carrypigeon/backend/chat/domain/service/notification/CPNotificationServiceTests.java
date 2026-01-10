package team.carrypigeon.backend.chat.domain.service.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.connection.notification.CPNotification;
import team.carrypigeon.backend.chat.domain.service.session.CPSessionCenterService;
import team.carrypigeon.backend.chat.domain.support.TestSession;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CPNotificationServiceTests {

    @Test
    void sendNotification_uidsNullOrEmpty_shouldReturnTrue() {
        CPSessionCenterService sessionCenterService = mock(CPSessionCenterService.class);
        CPNotificationService service = new CPNotificationService(sessionCenterService, new ObjectMapper());

        assertTrue(service.sendNotification(null, new CPNotification()));
        assertTrue(service.sendNotification(Set.of(), new CPNotification()));
        verifyNoInteractions(sessionCenterService);
    }

    @Test
    void sendNotification_noSession_shouldSkip() {
        CPSessionCenterService sessionCenterService = mock(CPSessionCenterService.class);
        when(sessionCenterService.getSessions(1L)).thenReturn(null);

        CPNotificationService service = new CPNotificationService(sessionCenterService, new ObjectMapper());
        assertTrue(service.sendNotification(Set.of(1L), new CPNotification().setRoute("/r")));
        verify(sessionCenterService).getSessions(1L);
    }

    @Test
    void sendNotification_happyPath_shouldWriteToAllSessions() throws Exception {
        CPSessionCenterService sessionCenterService = mock(CPSessionCenterService.class);
        TestSession s1 = new TestSession();
        TestSession s2 = new TestSession();
        when(sessionCenterService.getSessions(1L)).thenReturn(List.of(s1, s2));

        ObjectMapper mapper = new ObjectMapper();
        CPNotificationService service = new CPNotificationService(sessionCenterService, mapper);

        CPNotification notification = new CPNotification().setRoute("/r").setData(mapper.readTree("{\"x\":1}"));
        assertTrue(service.sendNotification(Set.of(1L), notification));

        assertEquals(1, s1.getWrittenMessages().size());
        assertEquals(1, s2.getWrittenMessages().size());

        var resp1 = mapper.readTree(s1.getWrittenMessages().get(0));
        assertEquals(-1, resp1.get("id").asLong());
        assertEquals(0, resp1.get("code").asInt());
        assertEquals("/r", resp1.get("data").get("route").asText());
        assertEquals(1, resp1.get("data").get("data").get("x").asInt());
    }

    @Test
    void sendNotification_objectMapperThrows_shouldCatchAndReturnTrue() throws Exception {
        CPSessionCenterService sessionCenterService = mock(CPSessionCenterService.class);
        TestSession s1 = new TestSession();
        when(sessionCenterService.getSessions(1L)).thenReturn(List.of(s1));

        ObjectMapper mapper = spy(new ObjectMapper());
        doThrow(new JsonProcessingException("boom") {}).when(mapper).writeValueAsString(any());

        CPNotificationService service = new CPNotificationService(sessionCenterService, mapper);
        assertTrue(service.sendNotification(Set.of(1L), new CPNotification().setRoute("/r")));
        assertTrue(s1.getWrittenMessages().isEmpty());
    }
}

