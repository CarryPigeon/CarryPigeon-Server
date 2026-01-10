package team.carrypigeon.backend.api.bo.domain.channel.member;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CPChannelMemberAuthorityEnumTests {

    @Test
    void valueOf_validValues_shouldReturnEnum() {
        assertEquals(CPChannelMemberAuthorityEnum.MEMBER, CPChannelMemberAuthorityEnum.valueOf(0));
        assertEquals(CPChannelMemberAuthorityEnum.ADMIN, CPChannelMemberAuthorityEnum.valueOf(1));
    }

    @Test
    void valueOf_invalidValue_shouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> CPChannelMemberAuthorityEnum.valueOf(-1));
    }
}

