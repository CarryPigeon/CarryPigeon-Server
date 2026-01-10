package team.carrypigeon.backend.api.bo.domain.channel.ban;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CPChannelBanTests {

    @Test
    void isValid_shouldReturnTrueWithinDuration() {
        CPChannelBan ban = new CPChannelBan()
                .setCreateTime(LocalDateTime.now().minusSeconds(1))
                .setDuration(10);
        assertTrue(ban.isValid());
    }

    @Test
    void isValid_shouldReturnFalseAfterDuration() {
        CPChannelBan ban = new CPChannelBan()
                .setCreateTime(LocalDateTime.now().minusSeconds(10))
                .setDuration(1);
        assertFalse(ban.isValid());
    }
}

