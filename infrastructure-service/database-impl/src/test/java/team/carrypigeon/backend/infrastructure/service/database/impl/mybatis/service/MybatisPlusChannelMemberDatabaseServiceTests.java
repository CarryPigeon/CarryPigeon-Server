package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.dao.DataRetrievalFailureException;
import team.carrypigeon.backend.infrastructure.service.database.api.exception.DatabaseServiceException;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelMemberRecord;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.ChannelMemberMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MybatisPlusChannelMemberDatabaseService 契约测试。
 * 职责：验证频道成员 MyBatis-Plus 数据库服务的关键读写契约与失败语义。
 * 边界：不访问真实数据库，只验证 mapper 交互后的稳定行为。
 */
@Tag("contract")
class MybatisPlusChannelMemberDatabaseServiceTests {

    /**
     * 验证存在成员关系时 exists 会返回 true。
     */
    @Test
    @DisplayName("exists existing membership returns true")
    void exists_existingMembership_returnsTrue() {
        ChannelMemberMapper channelMemberMapper = mock(ChannelMemberMapper.class);
        when(channelMemberMapper.countMembership(1L, 1001L)).thenReturn(1L);
        MybatisPlusChannelMemberDatabaseService service = new MybatisPlusChannelMemberDatabaseService(channelMemberMapper);

        assertTrue(service.exists(1L, 1001L));
    }

    /**
     * 验证插入成员关系时会写入复合键对应的完整字段。
     */
    @Test
    @DisplayName("insert valid record maps composite key fields")
    void insert_validRecord_mapsCompositeKeyFields() {
        ChannelMemberMapper channelMemberMapper = mock(ChannelMemberMapper.class);
        when(channelMemberMapper.insertMembership(1L, 1001L, Instant.parse("2026-04-22T00:00:00Z"))).thenReturn(1);
        MybatisPlusChannelMemberDatabaseService service = new MybatisPlusChannelMemberDatabaseService(channelMemberMapper);

        service.insert(new ChannelMemberRecord(1L, 1001L, Instant.parse("2026-04-22T00:00:00Z")));

        verify(channelMemberMapper).insertMembership(1L, 1001L, Instant.parse("2026-04-22T00:00:00Z"));
    }

    /**
     * 验证查询成员账户时会保持按 account_id 升序的语义。
     */
    @Test
    @DisplayName("find account ids existing rows keeps ordered account ids")
    void findAccountIdsByChannelId_existingRows_keepsOrderedAccountIds() {
        ChannelMemberMapper channelMemberMapper = mock(ChannelMemberMapper.class);
        when(channelMemberMapper.findAccountIdsByChannelId(1L)).thenReturn(List.of(1001L, 1002L));
        MybatisPlusChannelMemberDatabaseService service = new MybatisPlusChannelMemberDatabaseService(channelMemberMapper);

        List<Long> accountIds = service.findAccountIdsByChannelId(1L);

        assertEquals(List.of(1001L, 1002L), accountIds);
    }

    /**
     * 验证查询成员账户失败时会包装成稳定数据库服务异常。
     */
    @Test
    @DisplayName("find account ids data access failure wraps database service exception")
    void findAccountIdsByChannelId_dataAccessFailure_wrapsDatabaseServiceException() {
        ChannelMemberMapper channelMemberMapper = mock(ChannelMemberMapper.class);
        DataRetrievalFailureException cause = new DataRetrievalFailureException("database down");
        when(channelMemberMapper.findAccountIdsByChannelId(1L)).thenThrow(cause);
        MybatisPlusChannelMemberDatabaseService service = new MybatisPlusChannelMemberDatabaseService(channelMemberMapper);

        DatabaseServiceException exception = assertThrows(
                DatabaseServiceException.class,
                () -> service.findAccountIdsByChannelId(1L)
        );

        assertEquals("failed to query channel member account ids", exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}
