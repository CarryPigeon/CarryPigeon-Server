package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.dao.DataRetrievalFailureException;
import team.carrypigeon.backend.infrastructure.service.database.api.exception.DatabaseServiceException;
import team.carrypigeon.backend.infrastructure.service.database.api.model.UserProfileRecord;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.UserProfileEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.UserProfileMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MybatisPlusUserProfileDatabaseService 契约测试。
 * 职责：验证用户资料 MyBatis-Plus 数据库服务的关键失败路径处理。
 * 边界：不访问真实数据库，只验证 mapper 交互后的稳定异常语义。
 */
@Tag("contract")
class MybatisPlusUserProfileDatabaseServiceTests {

    private static final Instant CREATED_AT = Instant.parse("2026-04-20T12:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-04-21T12:00:00Z");

    /**
     * 验证按账户 ID 查询时会将数据库行稳定映射为契约记录。
     */
    @Test
    @DisplayName("find by account id existing row maps record")
    void findByAccountId_existingRow_mapsRecord() {
        UserProfileMapper userProfileMapper = mock(UserProfileMapper.class);
        when(userProfileMapper.selectById(1001L)).thenReturn(entity());
        MybatisPlusUserProfileDatabaseService service = new MybatisPlusUserProfileDatabaseService(userProfileMapper);

        UserProfileRecord record = service.findByAccountId(1001L).orElseThrow();

        assertEquals(1001L, record.accountId());
        assertEquals("carry-user", record.nickname());
        assertEquals("https://img.example/avatar.png", record.avatarUrl());
        assertEquals("hello world", record.bio());
        assertEquals(CREATED_AT, record.createdAt());
        assertEquals(UPDATED_AT, record.updatedAt());
    }

    /**
     * 验证查询底层数据库失败时会包装成稳定数据库服务异常。
     */
    @Test
    @DisplayName("find by account id data access failure wraps database service exception")
    void findByAccountId_dataAccessFailure_wrapsDatabaseServiceException() {
        UserProfileMapper userProfileMapper = mock(UserProfileMapper.class);
        DataRetrievalFailureException cause = new DataRetrievalFailureException("database down");
        when(userProfileMapper.selectById(1001L)).thenThrow(cause);
        MybatisPlusUserProfileDatabaseService service = new MybatisPlusUserProfileDatabaseService(userProfileMapper);

        DatabaseServiceException exception = assertThrows(
                DatabaseServiceException.class,
                () -> service.findByAccountId(1001L)
        );

        assertEquals("failed to query user profile by account id", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    /**
     * 验证插入资料时会写入完整持久化字段。
     */
    @Test
    @DisplayName("insert valid record maps all fields")
    void insert_validRecord_mapsAllFields() {
        UserProfileMapper userProfileMapper = mock(UserProfileMapper.class);
        when(userProfileMapper.insert(any(UserProfileEntity.class))).thenReturn(1);
        MybatisPlusUserProfileDatabaseService service = new MybatisPlusUserProfileDatabaseService(userProfileMapper);

        service.insert(record());

        verify(userProfileMapper).insert(any(UserProfileEntity.class));
    }

    /**
     * 验证插入底层数据库失败时会包装成稳定数据库服务异常。
     */
    @Test
    @DisplayName("insert data access failure wraps database service exception")
    void insert_dataAccessFailure_wrapsDatabaseServiceException() {
        UserProfileMapper userProfileMapper = mock(UserProfileMapper.class);
        DataRetrievalFailureException cause = new DataRetrievalFailureException("database down");
        when(userProfileMapper.insert(any(UserProfileEntity.class))).thenThrow(cause);
        MybatisPlusUserProfileDatabaseService service = new MybatisPlusUserProfileDatabaseService(userProfileMapper);

        DatabaseServiceException exception = assertThrows(
                DatabaseServiceException.class,
                () -> service.insert(record())
        );

        assertEquals("failed to insert user profile", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    /**
     * 验证更新影响一行时会正常完成。
     */
    @Test
    @DisplayName("update one affected row succeeds")
    void update_oneAffectedRow_succeeds() {
        UserProfileMapper userProfileMapper = mock(UserProfileMapper.class);
        when(userProfileMapper.updateById(any(UserProfileEntity.class))).thenReturn(1);
        MybatisPlusUserProfileDatabaseService service = new MybatisPlusUserProfileDatabaseService(userProfileMapper);

        service.update(record());

        verify(userProfileMapper).updateById(any(UserProfileEntity.class));
    }

    /**
     * 验证更新影响行数为零时会抛出稳定数据库服务异常。
     */
    @Test
    @DisplayName("update zero affected rows throws database service exception")
    void update_zeroAffectedRows_throwsDatabaseServiceException() {
        UserProfileMapper userProfileMapper = mock(UserProfileMapper.class);
        when(userProfileMapper.updateById(any(UserProfileEntity.class))).thenReturn(0);
        MybatisPlusUserProfileDatabaseService service = new MybatisPlusUserProfileDatabaseService(userProfileMapper);

        DatabaseServiceException exception = assertThrows(
                DatabaseServiceException.class,
                () -> service.update(record())
        );

        assertEquals("user profile update affected no rows", exception.getMessage());
    }

    /**
     * 验证更新底层数据库失败时会包装成稳定数据库服务异常。
     */
    @Test
    @DisplayName("update data access failure wraps database service exception")
    void update_dataAccessFailure_wrapsDatabaseServiceException() {
        UserProfileMapper userProfileMapper = mock(UserProfileMapper.class);
        DataRetrievalFailureException cause = new DataRetrievalFailureException("database down");
        when(userProfileMapper.updateById(any(UserProfileEntity.class))).thenThrow(cause);
        MybatisPlusUserProfileDatabaseService service = new MybatisPlusUserProfileDatabaseService(userProfileMapper);

        DatabaseServiceException exception = assertThrows(
                DatabaseServiceException.class,
                () -> service.update(record())
        );

        assertEquals("failed to update user profile", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    private static UserProfileRecord record() {
        return new UserProfileRecord(
                1001L,
                "carry-user",
                "https://img.example/avatar.png",
                "hello world",
                CREATED_AT,
                UPDATED_AT
        );
    }

    private static UserProfileEntity entity() {
        UserProfileEntity entity = new UserProfileEntity();
        entity.setAccountId(1001L);
        entity.setNickname("carry-user");
        entity.setAvatarUrl("https://img.example/avatar.png");
        entity.setBio("hello world");
        entity.setCreatedAt(CREATED_AT);
        entity.setUpdatedAt(UPDATED_AT);
        return entity;
    }
}
