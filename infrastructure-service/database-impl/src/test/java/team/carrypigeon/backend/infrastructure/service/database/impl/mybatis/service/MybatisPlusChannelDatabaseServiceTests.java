package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.dao.DataRetrievalFailureException;
import team.carrypigeon.backend.infrastructure.service.database.api.exception.DatabaseServiceException;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelRecord;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.ChannelMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * MybatisPlusChannelDatabaseService 契约测试。
 * 职责：验证频道 MyBatis-Plus 数据库服务的关键查询契约与失败语义。
 * 边界：不访问真实数据库，只验证 mapper 交互后的稳定行为。
 */
@Tag("contract")
class MybatisPlusChannelDatabaseServiceTests {

    /**
     * 验证查询默认频道时会稳定映射频道记录。
     */
    @Test
    @DisplayName("find default channel existing row maps record")
    void findDefaultChannel_existingRow_mapsRecord() {
        ChannelMapper channelMapper = mock(ChannelMapper.class);
        when(channelMapper.selectOne(org.mockito.ArgumentMatchers.<LambdaQueryWrapper<ChannelEntity>>any())).thenReturn(entity());
        MybatisPlusChannelDatabaseService service = new MybatisPlusChannelDatabaseService(channelMapper);

        ChannelRecord record = service.findDefaultChannel().orElseThrow();

        assertEquals(1L, record.id());
        assertEquals("public", record.name());
        assertEquals(true, record.defaultChannel());
    }

    /**
     * 验证底层数据库失败时会包装成稳定数据库服务异常。
     */
    @Test
    @DisplayName("find channel by id data access failure wraps database service exception")
    void findById_dataAccessFailure_wrapsDatabaseServiceException() {
        ChannelMapper channelMapper = mock(ChannelMapper.class);
        DataRetrievalFailureException cause = new DataRetrievalFailureException("database down");
        when(channelMapper.selectById(1L)).thenThrow(cause);
        MybatisPlusChannelDatabaseService service = new MybatisPlusChannelDatabaseService(channelMapper);

        DatabaseServiceException exception = assertThrows(
                DatabaseServiceException.class,
                () -> service.findById(1L)
        );

        assertEquals("failed to query channel by id", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    /**
     * 验证写入频道记录时会委托给 mapper。
     */
    @Test
    @DisplayName("insert valid record delegates to mapper")
    void insert_validRecord_delegatesToMapper() {
        ChannelMapper channelMapper = mock(ChannelMapper.class);
        MybatisPlusChannelDatabaseService service = new MybatisPlusChannelDatabaseService(channelMapper);

        service.insert(new ChannelRecord(
                9L,
                9L,
                "project-alpha",
                "private",
                false,
                Instant.parse("2026-04-24T12:00:00Z"),
                Instant.parse("2026-04-24T12:00:00Z")
        ));

        verify(channelMapper).insert(org.mockito.ArgumentMatchers.<ChannelEntity>any());
    }

    private static ChannelEntity entity() {
        ChannelEntity entity = new ChannelEntity();
        entity.setId(1L);
        entity.setConversationId(1L);
        entity.setName("public");
        entity.setType("public");
        entity.setDefaultChannel(true);
        entity.setCreatedAt(Instant.parse("2026-04-22T00:00:00Z"));
        entity.setUpdatedAt(Instant.parse("2026-04-22T00:00:00Z"));
        return entity;
    }
}
