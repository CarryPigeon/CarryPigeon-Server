package team.carrypigeon.backend.connection.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KeyPackTests {

    @Test
    void aesKeyPack_gettersSetters_shouldWork() {
        CPAESKeyPack ctorPack = new CPAESKeyPack(9L, 10L, "ck");
        assertEquals(9L, ctorPack.getId());
        assertEquals(10L, ctorPack.getSessionId());
        assertEquals("ck", ctorPack.getKey());

        CPAESKeyPack pack = new CPAESKeyPack();
        pack.setId(1L);
        pack.setSessionId(2L);
        pack.setKey("k");

        assertEquals(1L, pack.getId());
        assertEquals(2L, pack.getSessionId());
        assertEquals("k", pack.getKey());
    }

    @Test
    void eccKeyPack_gettersSetters_shouldWork() {
        CPECCKeyPack ctorPack = new CPECCKeyPack(7L, "pub2");
        assertEquals(7L, ctorPack.getId());
        assertEquals("pub2", ctorPack.getKey());

        CPECCKeyPack pack = new CPECCKeyPack();
        pack.setId(3L);
        pack.setKey("pub");

        assertEquals(3L, pack.getId());
        assertEquals("pub", pack.getKey());
    }
}
