package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataRetrievalFailureException;
import team.carrypigeon.backend.infrastructure.service.database.api.exception.DatabaseServiceException;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelInviteRecord;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelInviteEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.ChannelInviteMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MybatisPlusChannelInviteDatabaseService 契约测试。
 * 职责：验证频道邀请 MyBatis 数据库服务的关键读写契约与失败语义。
 * 边界：不访问真实数据库，只验证 mapper 交互后的稳定行为。
 */
@Tag("contract")
class MybatisPlusChannelInviteDatabaseServiceTests {

    /**
     * 验证查询邀请时会返回完整邀请记录。
     */
    @Test
    @DisplayName("find by channel and invitee existing row returns invite record")
    void findByChannelIdAndInviteeAccountId_existingRow_returnsInviteRecord() {
        ChannelInviteMapper channelInviteMapper = mock(ChannelInviteMapper.class);
        ChannelInviteEntity entity = new ChannelInviteEntity();
        entity.setChannelId(1L);
        entity.setInviteeAccountId(1002L);
        entity.setInviterAccountId(1001L);
        entity.setStatus("PENDING");
        entity.setCreatedAt(Instant.parse("2026-04-24T12:10:00Z"));
        when(channelInviteMapper.findByChannelIdAndInviteeAccountId(1L, 1002L)).thenReturn(entity);
        MybatisPlusChannelInviteDatabaseService service = new MybatisPlusChannelInviteDatabaseService(channelInviteMapper);

        ChannelInviteRecord record = service.findByChannelIdAndInviteeAccountId(1L, 1002L).orElseThrow();

        assertEquals("PENDING", record.status());
        assertEquals(1001L, record.inviterAccountId());
    }

    /**
     * 验证写入邀请时会下发给 mapper。
     */
    @Test
    @DisplayName("insert valid record delegates to mapper")
    void insert_validRecord_delegatesToMapper() {
        ChannelInviteMapper channelInviteMapper = mock(ChannelInviteMapper.class);
        MybatisPlusChannelInviteDatabaseService service = new MybatisPlusChannelInviteDatabaseService(channelInviteMapper);

        service.insert(new ChannelInviteRecord(1L, 1002L, 1001L, "PENDING", Instant.parse("2026-04-24T12:10:00Z"), null));

        verify(channelInviteMapper).insert(any());
    }

    /**
     * 验证更新邀请失败时会包装成稳定数据库服务异常。
     */
    @Test
    @DisplayName("update data access failure wraps database service exception")
    void update_dataAccessFailure_wrapsDatabaseServiceException() {
        ChannelInviteMapper channelInviteMapper = mock(ChannelInviteMapper.class);
        DataRetrievalFailureException cause = new DataRetrievalFailureException("database down");
        when(channelInviteMapper.update(any())).thenThrow(cause);
        MybatisPlusChannelInviteDatabaseService service = new MybatisPlusChannelInviteDatabaseService(channelInviteMapper);

        DatabaseServiceException exception = assertThrows(
                DatabaseServiceException.class,
                () -> service.update(new ChannelInviteRecord(1L, 1002L, 1001L, "ACCEPTED", Instant.parse("2026-04-24T12:10:00Z"), Instant.parse("2026-04-24T12:11:00Z")))
        );

        assertEquals("failed to update channel invite", exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}
