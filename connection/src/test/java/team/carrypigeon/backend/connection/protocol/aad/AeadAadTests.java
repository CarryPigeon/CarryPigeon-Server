package team.carrypigeon.backend.connection.protocol.aad;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AeadAadTests {

    @Test
    void encodeDecode_roundTrip_shouldPreserveFields() {
        AeadAad aad = new AeadAad(12, 34L, 56L);
        byte[] encoded = aad.encode();
        assertNotNull(encoded);
        assertEquals(AeadAad.LENGTH, encoded.length);

        AeadAad decoded = AeadAad.decode(encoded);
        assertEquals(12, decoded.getPackageId());
        assertEquals(34L, decoded.getSessionId());
        assertEquals(56L, decoded.getTimestampMillis());
    }

    @Test
    void decode_invalidLength_shouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> AeadAad.decode(new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> AeadAad.decode(new byte[AeadAad.LENGTH - 1]));
        assertThrows(IllegalArgumentException.class, () -> AeadAad.decode(new byte[AeadAad.LENGTH + 1]));
    }

    @Test
    void decode_negativeFields_shouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> AeadAad.decode(new AeadAad(-1, 1L, 1L).encode()));
        assertThrows(IllegalArgumentException.class, () -> AeadAad.decode(new AeadAad(1, -1L, 1L).encode()));
        assertThrows(IllegalArgumentException.class, () -> AeadAad.decode(new AeadAad(1, 1L, -1L).encode()));
    }
}
