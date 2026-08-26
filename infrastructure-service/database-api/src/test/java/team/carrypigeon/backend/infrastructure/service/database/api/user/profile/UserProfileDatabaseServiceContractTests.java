package team.carrypigeon.backend.infrastructure.service.database.api.user.profile;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UserProfileDatabaseService 默认行为契约测试。
 * 职责：锁定批量资料查询默认实现的筛选与空输入语义。
 * 边界：抽象数据库操作由替身提供，只验证接口中实际存在的默认逻辑。
 */
@Tag("contract")
class UserProfileDatabaseServiceContractTests {

    private static final UserProfileRecord MATCHED_RECORD = new UserProfileRecord(
            1001L,
            "carry-user",
            "https://img.example/avatar.png",
            "hello world",
            1L,
            20260420L,
            Instant.parse("2026-04-20T12:00:00Z"),
            Instant.parse("2026-04-21T12:00:00Z")
    );

    private static final UserProfileRecord OTHER_RECORD = new UserProfileRecord(
            1002L,
            "other-user",
            "",
            "",
            0L,
            0L,
            MATCHED_RECORD.createdAt(),
            MATCHED_RECORD.updatedAt()
    );

    /**
     * 验证默认批量查询只保留目标账户对应的资料。
     */
    @Test
    @DisplayName("find by account ids selected accounts returns matching profiles")
    void findByAccountIds_selectedAccounts_returnsMatchingProfiles() {
        UserProfileDatabaseService service = mock(UserProfileDatabaseService.class, CALLS_REAL_METHODS);
        when(service.findAll()).thenReturn(List.of(MATCHED_RECORD, OTHER_RECORD));

        List<UserProfileRecord> result = service.findByAccountIds(List.of(1001L));

        assertEquals(List.of(MATCHED_RECORD), result);
        verify(service).findAll();
    }

    /**
     * 验证默认批量查询对 null 和空集合直接返回空结果，且不访问数据库。
     */
    @Test
    @DisplayName("find by account ids empty input returns empty list")
    void findByAccountIds_emptyInput_returnsEmptyList() {
        UserProfileDatabaseService service = mock(UserProfileDatabaseService.class, CALLS_REAL_METHODS);

        assertEquals(List.of(), service.findByAccountIds(null));
        assertEquals(List.of(), service.findByAccountIds(List.of()));
        verify(service, never()).findAll();
    }
}
