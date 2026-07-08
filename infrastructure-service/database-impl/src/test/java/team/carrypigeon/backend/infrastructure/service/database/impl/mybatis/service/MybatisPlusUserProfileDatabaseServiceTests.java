package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataRetrievalFailureException;
import team.carrypigeon.backend.infrastructure.service.database.api.exception.DatabaseServiceException;
import team.carrypigeon.backend.infrastructure.service.database.api.user.profile.UserProfileRecord;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.user.profile.UserProfileEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.user.profile.UserProfileMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.user.profile.MybatisPlusUserProfileDatabaseService;

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
        assertEquals(1L, record.sex());
        assertEquals(20260420L, record.birthday());
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
     * 验证查询全部资料时会按记录映射返回结果。
     */
    @Test
    @DisplayName("find all maps all rows")
    void findAll_mapsAllRows() {
        UserProfileMapper userProfileMapper = mock(UserProfileMapper.class);
        when(userProfileMapper.selectList(any())).thenReturn(java.util.List.of(entity()));
        MybatisPlusUserProfileDatabaseService service = new MybatisPlusUserProfileDatabaseService(userProfileMapper);

        java.util.List<UserProfileRecord> records = service.findAll();

        assertEquals(1, records.size());
        assertEquals(1001L, records.get(0).accountId());
    }

    /**
     * 验证按账户 ID 集合查询会返回批量查询结果。
     */
    @Test
    @DisplayName("find by account ids maps rows")
    void findByAccountIds_mapsRows() {
        UserProfileMapper userProfileMapper = mock(UserProfileMapper.class);
        when(userProfileMapper.selectList(any())).thenReturn(java.util.List.of(entity()));
        MybatisPlusUserProfileDatabaseService service = new MybatisPlusUserProfileDatabaseService(userProfileMapper);

        java.util.List<UserProfileRecord> records = service.findByAccountIds(java.util.List.of(1001L, 1002L));

        assertEquals(1, records.size());
        assertEquals(1001L, records.get(0).accountId());
    }

    /**
     * 验证空账户 ID 集合不会访问 mapper。
     */
    @Test
    @DisplayName("find by account ids empty returns empty list")
    void findByAccountIds_empty_returnsEmptyList() {
        UserProfileMapper userProfileMapper = mock(UserProfileMapper.class);
        MybatisPlusUserProfileDatabaseService service = new MybatisPlusUserProfileDatabaseService(userProfileMapper);

        java.util.List<UserProfileRecord> records = service.findByAccountIds(java.util.List.of());

        assertEquals(0, records.size());
    }

    /**
     * 验证按游标查询分页资料时会返回记录列表。
     */
    @Test
    @DisplayName("find by account id before maps rows")
    void findByAccountIdBefore_mapsRows() {
        UserProfileMapper userProfileMapper = mock(UserProfileMapper.class);
        when(userProfileMapper.selectList(any())).thenReturn(java.util.List.of(entity()));
        MybatisPlusUserProfileDatabaseService service = new MybatisPlusUserProfileDatabaseService(userProfileMapper);

        java.util.List<UserProfileRecord> records = service.findByAccountIdBefore(1002L, 20);

        assertEquals(1, records.size());
        assertEquals(1001L, records.get(0).accountId());
    }

    /**
     * 验证关键字搜索资料时会返回记录列表。
     */
    @Test
    @DisplayName("search by keyword maps rows")
    void searchByKeyword_mapsRows() {
        UserProfileMapper userProfileMapper = mock(UserProfileMapper.class);
        when(userProfileMapper.selectList(any())).thenReturn(java.util.List.of(entity()));
        MybatisPlusUserProfileDatabaseService service = new MybatisPlusUserProfileDatabaseService(userProfileMapper);

        java.util.List<UserProfileRecord> records = service.searchByKeyword("carry", null, 20);

        assertEquals(1, records.size());
        assertEquals(1001L, records.get(0).accountId());
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

        ArgumentCaptor<UserProfileEntity> captor = ArgumentCaptor.forClass(UserProfileEntity.class);
        verify(userProfileMapper).insert(captor.capture());
        UserProfileEntity entity = captor.getValue();
        assertEquals(1001L, entity.getAccountId());
        assertEquals("carry-user", entity.getNickname());
        assertEquals("https://img.example/avatar.png", entity.getAvatarUrl());
        assertEquals("hello world", entity.getBio());
        assertEquals(1L, entity.getSex());
        assertEquals(20260420L, entity.getBirthday());
        assertEquals(CREATED_AT, entity.getCreatedAt());
        assertEquals(UPDATED_AT, entity.getUpdatedAt());
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

        ArgumentCaptor<UserProfileEntity> captor = ArgumentCaptor.forClass(UserProfileEntity.class);
        verify(userProfileMapper).updateById(captor.capture());
        UserProfileEntity entity = captor.getValue();
        assertEquals(1001L, entity.getAccountId());
        assertEquals("carry-user", entity.getNickname());
        assertEquals("https://img.example/avatar.png", entity.getAvatarUrl());
        assertEquals("hello world", entity.getBio());
        assertEquals(1L, entity.getSex());
        assertEquals(20260420L, entity.getBirthday());
        assertEquals(CREATED_AT, entity.getCreatedAt());
        assertEquals(UPDATED_AT, entity.getUpdatedAt());
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
                1L,
                20260420L,
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
        entity.setSex(1L);
        entity.setBirthday(20260420L);
        entity.setCreatedAt(CREATED_AT);
        entity.setUpdatedAt(UPDATED_AT);
        return entity;
    }
}
