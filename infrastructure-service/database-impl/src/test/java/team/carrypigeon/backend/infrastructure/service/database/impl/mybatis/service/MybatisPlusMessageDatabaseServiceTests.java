package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.dao.DataRetrievalFailureException;
import team.carrypigeon.backend.infrastructure.service.database.api.exception.DatabaseServiceException;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MessageRecord;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.MessageEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.MessageMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MybatisPlusMessageDatabaseService 契约测试。
 * 职责：验证消息 MyBatis-Plus 数据库服务的关键读写契约与失败语义。
 * 边界：不访问真实数据库，只验证 mapper 交互后的稳定行为。
 */
@Tag("contract")
class MybatisPlusMessageDatabaseServiceTests {

    /**
     * 验证插入消息时会映射完整通用消息字段。
     */
    @Test
    @DisplayName("insert valid record maps all fields")
    void insert_validRecord_mapsAllFields() {
        MessageMapper messageMapper = mock(MessageMapper.class);
        when(messageMapper.insert(any(MessageEntity.class))).thenReturn(1);
        MybatisPlusMessageDatabaseService service = new MybatisPlusMessageDatabaseService(messageMapper);

        service.insert(record());

        verify(messageMapper).insert(any(MessageEntity.class));
    }

    /**
     * 验证按频道查询历史消息时会保留游标查询结果顺序。
     */
    @Test
    @DisplayName("find by channel id before existing rows maps records")
    void findByChannelIdBefore_existingRows_mapsRecords() {
        MessageMapper messageMapper = mock(MessageMapper.class);
        when(messageMapper.findByChannelIdBefore(1L, 4999L, 20)).thenReturn(List.of(entity()));
        MybatisPlusMessageDatabaseService service = new MybatisPlusMessageDatabaseService(messageMapper);

        MessageRecord record = service.findByChannelIdBefore(1L, 4999L, 20).getFirst();

        assertEquals(5001L, record.messageId());
        assertEquals("carrypigeon-local", record.serverId());
        assertEquals("[文本消息] hello world", record.previewText());
    }

    /**
     * 验证按频道搜索消息时会稳定映射搜索结果。
     */
    @Test
    @DisplayName("search by channel id existing rows maps records")
    void searchByChannelId_existingRows_mapsRecords() {
        MessageMapper messageMapper = mock(MessageMapper.class);
        when(messageMapper.searchByChannelId(1L, "hello", 20)).thenReturn(List.of(entity()));
        MybatisPlusMessageDatabaseService service = new MybatisPlusMessageDatabaseService(messageMapper);

        MessageRecord record = service.searchByChannelId(1L, "hello", 20).getFirst();

        assertEquals(5001L, record.messageId());
        assertEquals("hello world", record.searchableText());
    }

    /**
     * 验证查询失败时会包装成稳定数据库服务异常。
     */
    @Test
    @DisplayName("find by channel id before data access failure wraps database service exception")
    void findByChannelIdBefore_dataAccessFailure_wrapsDatabaseServiceException() {
        MessageMapper messageMapper = mock(MessageMapper.class);
        DataRetrievalFailureException cause = new DataRetrievalFailureException("database down");
        when(messageMapper.findByChannelIdBefore(1L, null, 20)).thenThrow(cause);
        MybatisPlusMessageDatabaseService service = new MybatisPlusMessageDatabaseService(messageMapper);

        DatabaseServiceException exception = assertThrows(
                DatabaseServiceException.class,
                () -> service.findByChannelIdBefore(1L, null, 20)
        );

        assertEquals("failed to query channel messages", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    /**
     * 验证搜索失败时会包装成稳定数据库服务异常。
     */
    @Test
    @DisplayName("search by channel id data access failure wraps database service exception")
    void searchByChannelId_dataAccessFailure_wrapsDatabaseServiceException() {
        MessageMapper messageMapper = mock(MessageMapper.class);
        DataRetrievalFailureException cause = new DataRetrievalFailureException("database down");
        when(messageMapper.searchByChannelId(1L, "hello", 20)).thenThrow(cause);
        MybatisPlusMessageDatabaseService service = new MybatisPlusMessageDatabaseService(messageMapper);

        DatabaseServiceException exception = assertThrows(
                DatabaseServiceException.class,
                () -> service.searchByChannelId(1L, "hello", 20)
        );

        assertEquals("failed to search channel messages", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    private static MessageRecord record() {
        return new MessageRecord(
                5001L,
                "carrypigeon-local",
                1L,
                1L,
                1001L,
                "text",
                "hello world",
                "[文本消息] hello world",
                "hello world",
                null,
                null,
                "sent",
                Instant.parse("2026-04-22T00:00:00Z")
        );
    }

    private static MessageEntity entity() {
        MessageEntity entity = new MessageEntity();
        entity.setMessageId(5001L);
        entity.setServerId("carrypigeon-local");
        entity.setConversationId(1L);
        entity.setChannelId(1L);
        entity.setSenderId(1001L);
        entity.setMessageType("text");
        entity.setBody("hello world");
        entity.setPreviewText("[文本消息] hello world");
        entity.setSearchableText("hello world");
        entity.setPayload(null);
        entity.setMetadata(null);
        entity.setStatus("sent");
        entity.setCreatedAt(Instant.parse("2026-04-22T00:00:00Z"));
        return entity;
    }
}
