package team.carrypigeon.backend.api.bo.domain.channel.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CPChannelApplicationStateEnumTests {

    @Test
    void valueOf_validValues_shouldReturnEnum() {
        assertEquals(CPChannelApplicationStateEnum.PENDING, CPChannelApplicationStateEnum.valueOf(0));
        assertEquals(CPChannelApplicationStateEnum.APPROVED, CPChannelApplicationStateEnum.valueOf(1));
        assertEquals(CPChannelApplicationStateEnum.REJECTED, CPChannelApplicationStateEnum.valueOf(2));
    }

    @Test
    void valueOf_invalidValue_shouldReturnNull() {
        assertNull(CPChannelApplicationStateEnum.valueOf(-1));
        assertNull(CPChannelApplicationStateEnum.valueOf(3));
    }
}

