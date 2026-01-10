package team.carrypigeon.backend.chat.domain.service.session;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.support.TestSession;

import static org.junit.jupiter.api.Assertions.*;

class CPSessionCenterServiceTests {

    @Test
    void addRemoveClean_shouldMaintainSessions() {
        CPSessionCenterService service = new CPSessionCenterService();
        TestSession s1 = new TestSession();
        TestSession s2 = new TestSession();

        service.addSession(1L, null);
        assertNull(service.getSessions(1L));

        service.addSession(1L, s1);
        service.addSession(1L, s2);
        assertNotNull(service.getSessions(1L));
        assertEquals(2, service.getSessions(1L).size());

        service.removeSession(1L, s1);
        assertEquals(1, service.getSessions(1L).size());

        service.removeSession(1L, s2);
        assertEquals(0, service.getSessions(1L).size());

        service.clean();
        assertNull(service.getSessions(1L));
    }
}

