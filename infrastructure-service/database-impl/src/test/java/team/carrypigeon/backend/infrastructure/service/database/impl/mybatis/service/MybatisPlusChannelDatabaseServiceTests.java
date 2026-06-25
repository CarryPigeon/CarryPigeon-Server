package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.dao.DataRetrievalFailureException;
import org.mockito.ArgumentCaptor;
import team.carrypigeon.backend.infrastructure.service.database.api.exception.DatabaseServiceException;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelDiscoverRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelRecord;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelDiscoverProjection;
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
        ChannelRecord record = new ChannelRecord(
                9L,
                9L,
                "project-alpha",
                "讨论区",
                "avatars/ch/9.png",
                "private",
                false,
                Instant.parse("2026-04-24T12:00:00Z"),
                Instant.parse("2026-04-24T12:00:00Z")
        );

        service.insert(record);

        ArgumentCaptor<ChannelEntity> captor = ArgumentCaptor.forClass(ChannelEntity.class);
        verify(channelMapper).insert(captor.capture());
        ChannelEntity entity = captor.getValue();
        assertEquals(record.id(), entity.getId());
        assertEquals(record.conversationId(), entity.getConversationId());
        assertEquals(record.name(), entity.getName());
        assertEquals(record.brief(), entity.getBrief());
        assertEquals(record.avatar(), entity.getAvatar());
        assertEquals(record.type(), entity.getType());
        assertEquals(record.defaultChannel(), entity.getDefaultChannel());
        assertEquals(record.createdAt(), entity.getCreatedAt());
        assertEquals(record.updatedAt(), entity.getUpdatedAt());
    }

    @Test
    @DisplayName("update valid record delegates to mapper")
    void update_validRecord_delegatesToMapper() {
        ChannelMapper channelMapper = mock(ChannelMapper.class);
        when(channelMapper.updateById(any(ChannelEntity.class))).thenReturn(1);
        MybatisPlusChannelDatabaseService service = new MybatisPlusChannelDatabaseService(channelMapper);
        ChannelRecord record = new ChannelRecord(
                9L,
                9L,
                "project-alpha",
                "讨论区",
                "avatars/ch/9.png",
                "private",
                false,
                Instant.parse("2026-04-24T12:00:00Z"),
                Instant.parse("2026-04-24T12:30:00Z")
        );

        service.update(record);

        ArgumentCaptor<ChannelEntity> captor = ArgumentCaptor.forClass(ChannelEntity.class);
        verify(channelMapper).updateById(captor.capture());
        ChannelEntity entity = captor.getValue();
        assertEquals(record.id(), entity.getId());
        assertEquals(record.conversationId(), entity.getConversationId());
        assertEquals(record.name(), entity.getName());
        assertEquals(record.brief(), entity.getBrief());
        assertEquals(record.avatar(), entity.getAvatar());
        assertEquals(record.type(), entity.getType());
        assertEquals(record.defaultChannel(), entity.getDefaultChannel());
        assertEquals(record.createdAt(), entity.getCreatedAt());
        assertEquals(record.updatedAt(), entity.getUpdatedAt());
    }

    /**
     * 验证删除频道时会调用 mapper 删除入口。
     */
    @Test
    @DisplayName("delete valid id delegates to mapper")
    void delete_validId_delegatesToMapper() {
        ChannelMapper channelMapper = mock(ChannelMapper.class);
        when(channelMapper.deleteById(9L)).thenReturn(1);
        MybatisPlusChannelDatabaseService service = new MybatisPlusChannelDatabaseService(channelMapper);

        service.delete(9L);

        verify(channelMapper).deleteById(9L);
    }

    /**
     * 验证查询 system 频道时会稳定映射频道记录。
     */
    @Test
    @DisplayName("find system channel existing row maps record")
    void findSystemChannel_existingRow_mapsRecord() {
        ChannelMapper channelMapper = mock(ChannelMapper.class);
        ChannelEntity systemEntity = entity();
        systemEntity.setId(2L);
        systemEntity.setConversationId(2L);
        systemEntity.setName("system");
        systemEntity.setType("system");
        systemEntity.setDefaultChannel(false);
        when(channelMapper.selectOne(org.mockito.ArgumentMatchers.<LambdaQueryWrapper<ChannelEntity>>any())).thenReturn(systemEntity);
        MybatisPlusChannelDatabaseService service = new MybatisPlusChannelDatabaseService(channelMapper);

        ChannelRecord record = service.findSystemChannel().orElseThrow();

        assertEquals(2L, record.id());
        assertEquals("system", record.name());
        assertEquals("system", record.type());
    }

    /**
     * 验证 discover 查询会映射成独立的 discover 读侧契约。
     */
    @Test
    @DisplayName("discover channels existing rows maps discover records")
    void discoverChannels_existingRows_mapsDiscoverRecords() {
        ChannelMapper channelMapper = mock(ChannelMapper.class);
        ChannelDiscoverProjection projection = new ChannelDiscoverProjection();
        projection.setId(9L);
        projection.setName("general");
        projection.setBrief("讨论区");
        projection.setAvatar("avatars/ch/9.png");
        projection.setMemberCount(42L);
        projection.setRequiresApplication(Boolean.FALSE);
        when(channelMapper.discoverChannels("gen", 10L, "public", 20)).thenReturn(List.of(projection));
        MybatisPlusChannelDatabaseService service = new MybatisPlusChannelDatabaseService(channelMapper);

        ChannelDiscoverRecord record = service.discoverChannels("gen", 10L, "public", 20).getFirst();

        assertEquals(9L, record.id());
        assertEquals(42L, record.memberCount());
        assertEquals(false, record.requiresApplication());
    }

    private static ChannelEntity entity() {
        ChannelEntity entity = new ChannelEntity();
        entity.setId(1L);
        entity.setConversationId(1L);
        entity.setName("public");
        entity.setBrief("");
        entity.setAvatar("");
        entity.setType("public");
        entity.setDefaultChannel(true);
        entity.setCreatedAt(Instant.parse("2026-04-22T00:00:00Z"));
        entity.setUpdatedAt(Instant.parse("2026-04-22T00:00:00Z"));
        return entity;
    }
}
