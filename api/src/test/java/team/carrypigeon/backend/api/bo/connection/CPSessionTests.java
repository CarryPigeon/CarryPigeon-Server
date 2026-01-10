package team.carrypigeon.backend.api.bo.connection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CPSessionTests {

    @Test
    void defaultWrite_shouldDelegateToEncryptedWrite() {
        class RecordingSession implements CPSession {
            private String msg;
            private Boolean encrypted;

            @Override
            public void write(String msg, boolean encrypted) {
                this.msg = msg;
                this.encrypted = encrypted;
            }

            @Override
            public <T> T getAttributeValue(String key, Class<T> type) {
                return null;
            }

            @Override
            public void setAttributeValue(String key, Object value) {
            }

            @Override
            public void close() {
            }
        }

        RecordingSession session = new RecordingSession();
        session.write("hello");
        assertEquals("hello", session.msg);
        assertEquals(true, session.encrypted);
    }
}

