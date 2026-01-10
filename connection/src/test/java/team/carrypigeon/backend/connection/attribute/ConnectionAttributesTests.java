package team.carrypigeon.backend.connection.attribute;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConnectionAttributesTests {

    @Test
    void constants_shouldBePresent() {
        new ConnectionAttributes();
        assertNotNull(ConnectionAttributes.SESSIONS);
        assertNotNull(ConnectionAttributes.ENCRYPTION_KEY);
        assertNotNull(ConnectionAttributes.ENCRYPTION_STATE);
        assertNotNull(ConnectionAttributes.PACKAGE_SESSION_ID);
        assertNotNull(ConnectionAttributes.PACKAGE_ID);
        assertNotNull(ConnectionAttributes.LOCAL_PACKAGE_ID);
        assertEquals("SESSIONS", ConnectionAttributes.SESSIONS.name());
    }
}
