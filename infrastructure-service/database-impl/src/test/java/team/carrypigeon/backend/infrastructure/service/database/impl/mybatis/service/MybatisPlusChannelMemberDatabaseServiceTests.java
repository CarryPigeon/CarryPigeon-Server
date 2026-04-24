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
        MybatisPlusChannelMemberDatabaseService service = new MybatisPlusChannelMemberDatabaseService(channelMemberMapper);

        service.insert(new ChannelMemberRecord(
                1L,
                1001L,
                "MEMBER",
                Instant.parse("2026-04-22T00:00:00Z"),
                Instant.parse("2026-04-22T00:05:00Z")
        ));

        verify(channelMemberMapper).insertMembership(any());
    }

    /**
     * 验证查询成员记录时会返回包含角色与禁言字段的完整契约。
     */
    @Test
    @DisplayName("find by channel and account existing row returns governance fields")
    void findByChannelIdAndAccountId_existingRow_returnsGovernanceFields() {
        ChannelMemberMapper channelMemberMapper = mock(ChannelMemberMapper.class);
        team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelMemberEntity entity =
                new team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelMemberEntity();
        entity.setChannelId(1L);
        entity.setAccountId(1001L);
        entity.setRole("ADMIN");
        entity.setJoinedAt(Instant.parse("2026-04-22T00:00:00Z"));
        entity.setMutedUntil(Instant.parse("2026-04-22T00:05:00Z"));
        when(channelMemberMapper.findByChannelIdAndAccountId(1L, 1001L)).thenReturn(entity);
        MybatisPlusChannelMemberDatabaseService service = new MybatisPlusChannelMemberDatabaseService(channelMemberMapper);

        ChannelMemberRecord record = service.findByChannelIdAndAccountId(1L, 1001L).orElseThrow();

        assertEquals("ADMIN", record.role());
        assertEquals(Instant.parse("2026-04-22T00:05:00Z"), record.mutedUntil());
    }

    /**
     * 验证更新成员记录失败时会包装成稳定数据库服务异常。
     */
    @Test
    @DisplayName("update data access failure wraps database service exception")
    void update_dataAccessFailure_wrapsDatabaseServiceException() {
        ChannelMemberMapper channelMemberMapper = mock(ChannelMemberMapper.class);
        DataRetrievalFailureException cause = new DataRetrievalFailureException("database down");
        when(channelMemberMapper.updateMembership(any())).thenThrow(cause);
        MybatisPlusChannelMemberDatabaseService service = new MybatisPlusChannelMemberDatabaseService(channelMemberMapper);

        DatabaseServiceException exception = assertThrows(
                DatabaseServiceException.class,
                () -> service.update(new ChannelMemberRecord(1L, 1001L, "ADMIN", Instant.parse("2026-04-22T00:00:00Z"), null))
        );

        assertEquals("failed to update channel membership", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    /**
     * 验证删除成员记录时会把复合键下发给 mapper。
     */
    @Test
    @DisplayName("delete valid key delegates to mapper")
    void delete_validKey_delegatesToMapper() {
        ChannelMemberMapper channelMemberMapper = mock(ChannelMemberMapper.class);
        MybatisPlusChannelMemberDatabaseService service = new MybatisPlusChannelMemberDatabaseService(channelMemberMapper);

        service.delete(1L, 1001L);

        verify(channelMemberMapper).deleteMembership(1L, 1001L);
    }

    /**
     * 验证删除成员记录失败时会包装成稳定数据库服务异常。
     */
    @Test
    @DisplayName("delete data access failure wraps database service exception")
    void delete_dataAccessFailure_wrapsDatabaseServiceException() {
        ChannelMemberMapper channelMemberMapper = mock(ChannelMemberMapper.class);
        DataRetrievalFailureException cause = new DataRetrievalFailureException("database down");
        when(channelMemberMapper.deleteMembership(1L, 1001L)).thenThrow(cause);
        MybatisPlusChannelMemberDatabaseService service = new MybatisPlusChannelMemberDatabaseService(channelMemberMapper);

        DatabaseServiceException exception = assertThrows(
                DatabaseServiceException.class,
                () -> service.delete(1L, 1001L)
        );

        assertEquals("failed to delete channel membership", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    /**
     * 验证按频道查询成员列表时会映射完整成员记录集合。
     */
    @Test
    @DisplayName("find by channel existing rows maps member records")
    void findByChannelId_existingRows_mapsMemberRecords() {
        ChannelMemberMapper channelMemberMapper = mock(ChannelMemberMapper.class);
        team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelMemberEntity ownerEntity =
                new team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelMemberEntity();
        ownerEntity.setChannelId(1L);
        ownerEntity.setAccountId(1001L);
        ownerEntity.setRole("OWNER");
        ownerEntity.setJoinedAt(Instant.parse("2026-04-22T00:00:00Z"));
        ownerEntity.setMutedUntil(null);
        team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelMemberEntity memberEntity =
                new team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelMemberEntity();
        memberEntity.setChannelId(1L);
        memberEntity.setAccountId(1002L);
        memberEntity.setRole("MEMBER");
        memberEntity.setJoinedAt(Instant.parse("2026-04-22T00:01:00Z"));
        memberEntity.setMutedUntil(null);
        when(channelMemberMapper.findByChannelId(1L)).thenReturn(List.of(ownerEntity, memberEntity));
        MybatisPlusChannelMemberDatabaseService service = new MybatisPlusChannelMemberDatabaseService(channelMemberMapper);

        List<ChannelMemberRecord> records = service.findByChannelId(1L);

        assertEquals(2, records.size());
        assertEquals("OWNER", records.get(0).role());
        assertEquals(1002L, records.get(1).accountId());
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
