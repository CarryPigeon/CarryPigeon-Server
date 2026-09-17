package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import team.carrypigeon.backend.infrastructure.service.database.api.model.NotificationChannelPreferenceRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.model.NotificationServerPreferenceRecord;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.NotificationChannelPreferenceEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.NotificationServerPreferenceEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.NotificationPreferenceMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
/**
 * `MybatisPlusNotificationPreferenceDatabaseService` 契约测试。
 * 职责：验证当前测试类覆盖对象的关键成功路径、失败路径或边界行为。
 */

@Tag("contract")
class MybatisPlusNotificationPreferenceDatabaseServiceTests {

    /**
     * 验证 `findServerPreference` 在 `mapsEntityToRecord` 场景下的测试契约。
     */
    @Test
    @DisplayName("find server preference maps entity to record")
    void findServerPreference_mapsEntityToRecord() {
        NotificationPreferenceMapper mapper = mock(NotificationPreferenceMapper.class);
        NotificationServerPreferenceEntity entity = new NotificationServerPreferenceEntity();
        entity.setAccountId(1001L);
        entity.setMode("all");
        entity.setMutedUntil(0L);
        entity.setCreatedAt(Instant.parse("2026-04-24T12:00:00Z"));
        entity.setUpdatedAt(Instant.parse("2026-04-24T12:00:00Z"));
        when(mapper.findServerPreferenceByAccountId(1001L)).thenReturn(entity);
        MybatisPlusNotificationPreferenceDatabaseService service = new MybatisPlusNotificationPreferenceDatabaseService(mapper);

        NotificationServerPreferenceRecord record = service.findServerPreferenceByAccountId(1001L).orElseThrow();

        assertEquals("all", record.mode());
        assertEquals(0L, record.mutedUntil());
    }

    /**
     * 验证 `listChannelPreferences` 在 `mapsEntitiesToRecords` 场景下的测试契约。
     */
    @Test
    @DisplayName("list channel preferences maps entities to records")
    void listChannelPreferences_mapsEntitiesToRecords() {
        NotificationPreferenceMapper mapper = mock(NotificationPreferenceMapper.class);
        NotificationChannelPreferenceEntity entity = new NotificationChannelPreferenceEntity();
        entity.setAccountId(1001L);
        entity.setChannelId(9L);
        entity.setMode("inherit");
        entity.setMutedUntil(0L);
        entity.setCreatedAt(Instant.parse("2026-04-24T12:00:00Z"));
        entity.setUpdatedAt(Instant.parse("2026-04-24T12:00:00Z"));
        when(mapper.listChannelPreferencesByAccountId(1001L)).thenReturn(List.of(entity));
        MybatisPlusNotificationPreferenceDatabaseService service = new MybatisPlusNotificationPreferenceDatabaseService(mapper);

        NotificationChannelPreferenceRecord record = service.listChannelPreferencesByAccountId(1001L).getFirst();

        assertEquals(9L, record.channelId());
        assertEquals("inherit", record.mode());
    }

    /**
     * 验证频道偏好按账户与频道复合键精确读取并映射记录。
     */
    @Test
    @DisplayName("find channel preference exact key maps entity")
    void findChannelPreference_exactKey_mapsEntity() {
        NotificationPreferenceMapper mapper = mock(NotificationPreferenceMapper.class);
        NotificationChannelPreferenceEntity entity = new NotificationChannelPreferenceEntity();
        entity.setAccountId(1001L);
        entity.setChannelId(9L);
        entity.setMode("muted");
        entity.setMutedUntil(100L);
        entity.setCreatedAt(Instant.parse("2026-04-24T12:00:00Z"));
        entity.setUpdatedAt(Instant.parse("2026-04-24T12:01:00Z"));
        when(mapper.findChannelPreference(1001L, 9L)).thenReturn(entity);
        MybatisPlusNotificationPreferenceDatabaseService service =
                new MybatisPlusNotificationPreferenceDatabaseService(mapper);

        NotificationChannelPreferenceRecord record = service.findChannelPreference(1001L, 9L).orElseThrow();

        assertEquals("muted", record.mode());
        assertEquals(100L, record.mutedUntil());
        verify(mapper).findChannelPreference(1001L, 9L);
    }

    /**
     * 验证 `upsertServerPreference` 在 `delegatesToMapper` 场景下的测试契约。
     */
    @Test
    @DisplayName("upsert server preference delegates to mapper")
    void upsertServerPreference_delegatesToMapper() {
        NotificationPreferenceMapper mapper = mock(NotificationPreferenceMapper.class);
        MybatisPlusNotificationPreferenceDatabaseService service = new MybatisPlusNotificationPreferenceDatabaseService(mapper);
        NotificationServerPreferenceRecord record = new NotificationServerPreferenceRecord(
                1001L,
                "muted",
                0L,
                Instant.parse("2026-04-24T12:00:00Z"),
                Instant.parse("2026-04-24T12:00:00Z")
        );

        service.upsertServerPreference(record);

        ArgumentCaptor<NotificationServerPreferenceEntity> captor = ArgumentCaptor.forClass(NotificationServerPreferenceEntity.class);
        verify(mapper).upsertServerPreference(captor.capture());
        NotificationServerPreferenceEntity entity = captor.getValue();
        assertEquals(record.accountId(), entity.getAccountId());
        assertEquals(record.mode(), entity.getMode());
        assertEquals(record.mutedUntil(), entity.getMutedUntil());
        assertEquals(record.createdAt(), entity.getCreatedAt());
        assertEquals(record.updatedAt(), entity.getUpdatedAt());
    }

    /**
     * 验证 `upsertChannelPreference` 在 `mapsAllFields` 场景下的测试契约。
     */
    @Test
    @DisplayName("upsert channel preference maps all fields")
    void upsertChannelPreference_mapsAllFields() {
        NotificationPreferenceMapper mapper = mock(NotificationPreferenceMapper.class);
        MybatisPlusNotificationPreferenceDatabaseService service = new MybatisPlusNotificationPreferenceDatabaseService(mapper);
        NotificationChannelPreferenceRecord record = new NotificationChannelPreferenceRecord(
                1001L,
                9L,
                "inherit",
                0L,
                Instant.parse("2026-04-24T12:00:00Z"),
                Instant.parse("2026-04-24T12:00:00Z")
        );

        service.upsertChannelPreference(record);

        ArgumentCaptor<NotificationChannelPreferenceEntity> captor = ArgumentCaptor.forClass(NotificationChannelPreferenceEntity.class);
        verify(mapper).upsertChannelPreference(captor.capture());
        NotificationChannelPreferenceEntity entity = captor.getValue();
        assertEquals(record.accountId(), entity.getAccountId());
        assertEquals(record.channelId(), entity.getChannelId());
        assertEquals(record.mode(), entity.getMode());
        assertEquals(record.mutedUntil(), entity.getMutedUntil());
        assertEquals(record.createdAt(), entity.getCreatedAt());
        assertEquals(record.updatedAt(), entity.getUpdatedAt());
    }

    /** 验证账户级偏好可以一次查询并按账户 ID 建立索引。 */
    @Test
    @DisplayName("find server preferences batches account ids")
    void findServerPreferencesByAccountIds_existingRows_indexesByAccountId() {
        NotificationPreferenceMapper mapper = mock(NotificationPreferenceMapper.class);
        NotificationServerPreferenceEntity entity = new NotificationServerPreferenceEntity();
        entity.setAccountId(1001L);
        entity.setMode("all");
        entity.setMutedUntil(0L);
        entity.setCreatedAt(Instant.parse("2026-04-24T12:00:00Z"));
        entity.setUpdatedAt(Instant.parse("2026-04-24T12:00:00Z"));
        when(mapper.findServerPreferencesByAccountIds(List.of(1001L, 1002L))).thenReturn(List.of(entity));
        MybatisPlusNotificationPreferenceDatabaseService service = new MybatisPlusNotificationPreferenceDatabaseService(mapper);

        Map<Long, NotificationServerPreferenceRecord> result = service.findServerPreferencesByAccountIds(List.of(1001L, 1002L));

        assertEquals("all", result.get(1001L).mode());
        assertEquals(1, result.size());
    }
}
