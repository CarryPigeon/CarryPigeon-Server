package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataRetrievalFailureException;
import team.carrypigeon.backend.infrastructure.service.database.api.exception.DatabaseServiceException;
import team.carrypigeon.backend.infrastructure.service.database.api.auth.account.AuthAccountRecord;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.auth.account.AuthAccountEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.auth.account.AuthAccountMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.auth.account.MybatisPlusAuthAccountDatabaseService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MybatisPlusAuthAccountDatabaseService 契约测试。
 * 职责：验证鉴权账户 MyBatis-Plus 数据库服务的关键查询、写入与失败语义。
 * 边界：不访问真实数据库，只验证 mapper 交互后的稳定行为。
 */
@Tag("contract")
class MybatisPlusAuthAccountDatabaseServiceTests {

    private static final Instant CREATED_AT = Instant.parse("2026-04-20T12:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-04-21T12:00:00Z");

    /**
     * 验证按用户名查询时会稳定映射账户记录。
     */
    @Test
    @DisplayName("find by username existing row maps record")
    void findByUsername_existingRow_mapsRecord() {
        AuthAccountMapper authAccountMapper = mock(AuthAccountMapper.class);
        when(authAccountMapper.selectOne(org.mockito.ArgumentMatchers.<LambdaQueryWrapper<AuthAccountEntity>>any()))
                .thenReturn(entity());
        MybatisPlusAuthAccountDatabaseService service = new MybatisPlusAuthAccountDatabaseService(authAccountMapper);

        AuthAccountRecord record = service.findByUsername("carry-user").orElseThrow();

        assertEquals(1001L, record.id());
        assertEquals("carry-user", record.username());
        assertEquals("hashed-password", record.passwordHash());
        assertEquals(CREATED_AT, record.createdAt());
        assertEquals(UPDATED_AT, record.updatedAt());
    }

    /**
     * 验证按账户 ID 查询时会稳定映射账户记录。
     */
    @Test
    @DisplayName("find by id existing row maps record")
    void findById_existingRow_mapsRecord() {
        AuthAccountMapper authAccountMapper = mock(AuthAccountMapper.class);
        when(authAccountMapper.selectById(1001L)).thenReturn(entity());
        MybatisPlusAuthAccountDatabaseService service = new MybatisPlusAuthAccountDatabaseService(authAccountMapper);

        AuthAccountRecord record = service.findById(1001L).orElseThrow();

        assertEquals(1001L, record.id());
        assertEquals("carry-user", record.username());
    }

    /**
     * 验证查询失败时会包装成稳定数据库服务异常。
     */
    @Test
    @DisplayName("find by username data access failure wraps database service exception")
    void findByUsername_dataAccessFailure_wrapsDatabaseServiceException() {
        AuthAccountMapper authAccountMapper = mock(AuthAccountMapper.class);
        DataRetrievalFailureException cause = new DataRetrievalFailureException("database down");
        when(authAccountMapper.selectOne(org.mockito.ArgumentMatchers.<LambdaQueryWrapper<AuthAccountEntity>>any()))
                .thenThrow(cause);
        MybatisPlusAuthAccountDatabaseService service = new MybatisPlusAuthAccountDatabaseService(authAccountMapper);

        DatabaseServiceException exception = assertThrows(
                DatabaseServiceException.class,
                () -> service.findByUsername("carry-user")
        );

        assertEquals("failed to query auth account by username", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    /**
     * 验证写入账户时会委托 mapper 持久化完整记录。
     */
    @Test
    @DisplayName("insert valid record maps all fields")
    void insert_validRecord_mapsAllFields() {
        AuthAccountMapper authAccountMapper = mock(AuthAccountMapper.class);
        when(authAccountMapper.insert(any(AuthAccountEntity.class))).thenReturn(1);
        MybatisPlusAuthAccountDatabaseService service = new MybatisPlusAuthAccountDatabaseService(authAccountMapper);

        service.insert(record());

        ArgumentCaptor<AuthAccountEntity> captor = ArgumentCaptor.forClass(AuthAccountEntity.class);
        verify(authAccountMapper).insert(captor.capture());
        AuthAccountEntity entity = captor.getValue();
        assertEquals(1001L, entity.getId());
        assertEquals("carry-user", entity.getUsername());
        assertEquals("hashed-password", entity.getPasswordHash());
        assertEquals(CREATED_AT, entity.getCreatedAt());
        assertEquals(UPDATED_AT, entity.getUpdatedAt());
    }

    /**
     * 验证写入失败时会包装成稳定数据库服务异常。
     */
    @Test
    @DisplayName("insert data access failure wraps database service exception")
    void insert_dataAccessFailure_wrapsDatabaseServiceException() {
        AuthAccountMapper authAccountMapper = mock(AuthAccountMapper.class);
        DataRetrievalFailureException cause = new DataRetrievalFailureException("database down");
        when(authAccountMapper.insert(any(AuthAccountEntity.class))).thenThrow(cause);
        MybatisPlusAuthAccountDatabaseService service = new MybatisPlusAuthAccountDatabaseService(authAccountMapper);

        DatabaseServiceException exception = assertThrows(
                DatabaseServiceException.class,
                () -> service.insert(record())
        );

        assertEquals("failed to insert auth account", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    /**
     * 验证更新账户时会委托 mapper 持久化完整记录。
     */
    @Test
    @DisplayName("update valid record maps all fields")
    void update_validRecord_mapsAllFields() {
        AuthAccountMapper authAccountMapper = mock(AuthAccountMapper.class);
        when(authAccountMapper.updateById(any(AuthAccountEntity.class))).thenReturn(1);
        MybatisPlusAuthAccountDatabaseService service = new MybatisPlusAuthAccountDatabaseService(authAccountMapper);

        service.update(record());

        ArgumentCaptor<AuthAccountEntity> captor = ArgumentCaptor.forClass(AuthAccountEntity.class);
        verify(authAccountMapper).updateById(captor.capture());
        AuthAccountEntity entity = captor.getValue();
        assertEquals(1001L, entity.getId());
        assertEquals("carry-user", entity.getUsername());
        assertEquals("hashed-password", entity.getPasswordHash());
        assertEquals(CREATED_AT, entity.getCreatedAt());
        assertEquals(UPDATED_AT, entity.getUpdatedAt());
    }

    private static AuthAccountRecord record() {
        return new AuthAccountRecord(1001L, "carry-user", "hashed-password", CREATED_AT, UPDATED_AT);
    }

    private static AuthAccountEntity entity() {
        AuthAccountEntity entity = new AuthAccountEntity();
        entity.setId(1001L);
        entity.setUsername("carry-user");
        entity.setPasswordHash("hashed-password");
        entity.setCreatedAt(CREATED_AT);
        entity.setUpdatedAt(UPDATED_AT);
        return entity;
    }
}
