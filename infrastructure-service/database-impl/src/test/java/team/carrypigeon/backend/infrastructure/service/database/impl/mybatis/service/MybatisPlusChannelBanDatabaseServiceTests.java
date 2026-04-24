package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataRetrievalFailureException;
import team.carrypigeon.backend.infrastructure.service.database.api.exception.DatabaseServiceException;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelBanRecord;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelBanEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.ChannelBanMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MybatisPlusChannelBanDatabaseService 契约测试。
 * 职责：验证频道封禁 MyBatis 数据库服务的关键读写契约与失败语义。
 * 边界：不访问真实数据库，只验证 mapper 交互后的稳定行为。
 */
@Tag("contract")
class MybatisPlusChannelBanDatabaseServiceTests {

    /**
     * 验证查询封禁时会返回完整封禁记录。
     */
    @Test
    @DisplayName("find by channel and banned account existing row returns ban record")
    void findByChannelIdAndBannedAccountId_existingRow_returnsBanRecord() {
        ChannelBanMapper channelBanMapper = mock(ChannelBanMapper.class);
        ChannelBanEntity entity = new ChannelBanEntity();
        entity.setChannelId(1L);
        entity.setBannedAccountId(1002L);
        entity.setOperatorAccountId(1001L);
        entity.setReason("spam");
        entity.setCreatedAt(Instant.parse("2026-04-24T12:20:00Z"));
        when(channelBanMapper.findByChannelIdAndBannedAccountId(1L, 1002L)).thenReturn(entity);
        MybatisPlusChannelBanDatabaseService service = new MybatisPlusChannelBanDatabaseService(channelBanMapper);

        ChannelBanRecord record = service.findByChannelIdAndBannedAccountId(1L, 1002L).orElseThrow();

        assertEquals("spam", record.reason());
        assertEquals(1001L, record.operatorAccountId());
    }

    /**
     * 验证写入封禁时会下发给 mapper。
     */
    @Test
    @DisplayName("insert valid record delegates to mapper")
    void insert_validRecord_delegatesToMapper() {
        ChannelBanMapper channelBanMapper = mock(ChannelBanMapper.class);
        MybatisPlusChannelBanDatabaseService service = new MybatisPlusChannelBanDatabaseService(channelBanMapper);

        service.insert(new ChannelBanRecord(1L, 1002L, 1001L, "spam", null, Instant.parse("2026-04-24T12:20:00Z"), null));

        verify(channelBanMapper).insert(any());
    }

    /**
     * 验证更新封禁失败时会包装成稳定数据库服务异常。
     */
    @Test
    @DisplayName("update data access failure wraps database service exception")
    void update_dataAccessFailure_wrapsDatabaseServiceException() {
        ChannelBanMapper channelBanMapper = mock(ChannelBanMapper.class);
        DataRetrievalFailureException cause = new DataRetrievalFailureException("database down");
        when(channelBanMapper.update(any())).thenThrow(cause);
        MybatisPlusChannelBanDatabaseService service = new MybatisPlusChannelBanDatabaseService(channelBanMapper);

        DatabaseServiceException exception = assertThrows(
                DatabaseServiceException.class,
                () -> service.update(new ChannelBanRecord(1L, 1002L, 1001L, "spam", null, Instant.parse("2026-04-24T12:20:00Z"), Instant.parse("2026-04-24T12:25:00Z")))
        );

        assertEquals("failed to update channel ban", exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}
