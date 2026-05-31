package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MentionRecord;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.MentionEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.MentionMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("contract")
class MybatisPlusMentionDatabaseServiceTests {

    @Test
    @DisplayName("list by account id maps entities to records")
    void listByAccountId_mapsEntitiesToRecords() {
        MentionMapper mapper = mock(MentionMapper.class);
        MentionEntity entity = new MentionEntity();
        entity.setMentionId(11L);
        entity.setChannelId(9L);
        entity.setMessageId(5001L);
        entity.setFromAccountId(1002L);
        entity.setTargetType("user");
        entity.setTargetAccountId(1001L);
        entity.setCreatedAt(Instant.parse("2026-04-24T12:00:00Z"));
        entity.setRead(Boolean.FALSE);
        when(mapper.listByAccountId(1001L, 88L, 20, true, 9L)).thenReturn(List.of(entity));
        MybatisPlusMentionDatabaseService service = new MybatisPlusMentionDatabaseService(mapper);

        MentionRecord record = service.listByAccountId(1001L, 88L, 20, true, 9L).getFirst();

        assertEquals(11L, record.mentionId());
        assertEquals(9L, record.channelId());
        assertEquals(false, record.read());
    }

    @Test
    @DisplayName("mark as read delegates to mapper")
    void markAsRead_delegatesToMapper() {
        MentionMapper mapper = mock(MentionMapper.class);
        when(mapper.markAsRead(1001L, 11L)).thenReturn(1);
        MybatisPlusMentionDatabaseService service = new MybatisPlusMentionDatabaseService(mapper);

        boolean result = service.markAsRead(1001L, 11L);

        assertEquals(true, result);
        verify(mapper).markAsRead(1001L, 11L);
    }

    @Test
    @DisplayName("mark all as read delegates to mapper")
    void markAllAsRead_delegatesToMapper() {
        MentionMapper mapper = mock(MentionMapper.class);
        when(mapper.markAllAsRead(1001L, 88L, 9L)).thenReturn(3);
        MybatisPlusMentionDatabaseService service = new MybatisPlusMentionDatabaseService(mapper);

        int result = service.markAllAsRead(1001L, 88L, 9L);

        assertEquals(3, result);
        verify(mapper).markAllAsRead(1001L, 88L, 9L);
    }
}
