package team.carrypigeon.backend.api.bo.domain.user;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CPUserSexEnumTests {

    @Test
    void valueOf_validValues_shouldReturnEnum() {
        assertEquals(CPUserSexEnum.UNKNOWN, CPUserSexEnum.valueOf(0));
        assertEquals(CPUserSexEnum.MALE, CPUserSexEnum.valueOf(1));
        assertEquals(CPUserSexEnum.FEMALE, CPUserSexEnum.valueOf(2));
    }

    @Test
    void valueOf_invalidValue_shouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> CPUserSexEnum.valueOf(-1));
    }
}

