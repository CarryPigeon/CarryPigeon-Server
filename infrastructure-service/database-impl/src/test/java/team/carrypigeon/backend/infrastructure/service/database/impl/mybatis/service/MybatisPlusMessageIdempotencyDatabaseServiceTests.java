package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataRetrievalFailureException;
import team.carrypigeon.backend.infrastructure.service.database.api.exception.DatabaseServiceException;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MessageIdempotencyRecord;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.MessageIdempotencyEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.MessageIdempotencyMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MyBatis 消息幂等数据库服务契约测试。
 * 职责：验证原子预留的调用顺序、字段映射和失败语义。
 * 边界：不访问真实数据库，并发锁语义由 Mapper SQL 契约测试保护。
 */
@Tag("contract")
class MybatisPlusMessageIdempotencyDatabaseServiceTests {

    private static final Instant BASE_TIME = Instant.parse("2026-07-21T00:00:00Z");

    /**
     * 验证预留先执行原子插入，再锁定读取当前持久化记录。
     */
    @Test
    @DisplayName("reserve valid request inserts then locks row")
    void reserve_validRequest_insertsThenLocksRow() {
        MessageIdempotencyMapper mapper = mock(MessageIdempotencyMapper.class);
        MessageIdempotencyEntity locked = entity(5001L);
        when(mapper.findForUpdate(1001L, "message.forward.v1", "forward-1")).thenReturn(locked);
        MybatisPlusMessageIdempotencyDatabaseService service = new MybatisPlusMessageIdempotencyDatabaseService(mapper);

        MessageIdempotencyRecord result = service.reserve(record());

        var ordered = inOrder(mapper);
        ordered.verify(mapper).reserve(any(MessageIdempotencyEntity.class));
        ordered.verify(mapper).findForUpdate(1001L, "message.forward.v1", "forward-1");
        assertEquals(5001L, result.messageId());
        assertEquals("fingerprint", result.requestFingerprint());
    }

    /**
     * 验证完成预留时严格携带请求指纹并要求唯一一行更新。
     */
    @Test
    @DisplayName("complete locked reservation binds message result")
    void complete_lockedReservation_bindsMessageResult() {
        MessageIdempotencyMapper mapper = mock(MessageIdempotencyMapper.class);
        when(mapper.complete(1001L, "message.forward.v1", "forward-1", "fingerprint", 5001L, BASE_TIME))
                .thenReturn(1);
        MybatisPlusMessageIdempotencyDatabaseService service = new MybatisPlusMessageIdempotencyDatabaseService(mapper);

        service.complete(1001L, "message.forward.v1", "forward-1", "fingerprint", 5001L, BASE_TIME);

        verify(mapper).complete(1001L, "message.forward.v1", "forward-1", "fingerprint", 5001L, BASE_TIME);
    }

    /**
     * 验证预留无法锁定记录时不会继续执行消息创建流程。
     */
    @Test
    @DisplayName("reserve missing locked row fails")
    void reserve_missingLockedRow_fails() {
        MessageIdempotencyMapper mapper = mock(MessageIdempotencyMapper.class);
        MybatisPlusMessageIdempotencyDatabaseService service = new MybatisPlusMessageIdempotencyDatabaseService(mapper);

        DatabaseServiceException exception = assertThrows(DatabaseServiceException.class, () -> service.reserve(record()));

        assertEquals("failed to lock message idempotency reservation", exception.getMessage());
    }

    /**
     * 验证数据库访问失败会转换为稳定的 database-api 异常。
     */
    @Test
    @DisplayName("reserve data access failure wraps exception")
    void reserve_dataAccessFailure_wrapsException() {
        MessageIdempotencyMapper mapper = mock(MessageIdempotencyMapper.class);
        DataRetrievalFailureException cause = new DataRetrievalFailureException("database down");
        when(mapper.reserve(any(MessageIdempotencyEntity.class))).thenThrow(cause);
        MybatisPlusMessageIdempotencyDatabaseService service = new MybatisPlusMessageIdempotencyDatabaseService(mapper);

        DatabaseServiceException exception = assertThrows(DatabaseServiceException.class, () -> service.reserve(record()));

        assertEquals("failed to reserve message idempotency key", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    /**
     * 验证结果已绑定或指纹不一致时完成操作失败。
     */
    @Test
    @DisplayName("complete unmatched reservation fails")
    void complete_unmatchedReservation_fails() {
        MessageIdempotencyMapper mapper = mock(MessageIdempotencyMapper.class);
        MybatisPlusMessageIdempotencyDatabaseService service = new MybatisPlusMessageIdempotencyDatabaseService(mapper);

        DatabaseServiceException exception = assertThrows(DatabaseServiceException.class, () ->
                service.complete(1001L, "message.forward.v1", "forward-1", "fingerprint", 5001L, BASE_TIME)
        );

        assertEquals("failed to complete message idempotency reservation", exception.getMessage());
    }

    private MessageIdempotencyRecord record() {
        return new MessageIdempotencyRecord(
                1001L, "message.forward.v1", "forward-1", "fingerprint", null, BASE_TIME, null
        );
    }

    private MessageIdempotencyEntity entity(Long messageId) {
        MessageIdempotencyEntity entity = new MessageIdempotencyEntity();
        entity.setAccountId(1001L);
        entity.setOperation("message.forward.v1");
        entity.setIdempotencyKey("forward-1");
        entity.setRequestFingerprint("fingerprint");
        entity.setMessageId(messageId);
        entity.setCreatedAt(BASE_TIME);
        entity.setCompletedAt(messageId == null ? null : BASE_TIME.plusSeconds(1));
        return entity;
    }
}
