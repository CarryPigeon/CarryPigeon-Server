package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.util.List;
import org.springframework.dao.DataAccessException;
import team.carrypigeon.backend.infrastructure.service.database.api.exception.DatabaseServiceException;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MentionRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.MentionDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.MentionEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.MentionMapper;

/**
 * MyBatis-Plus 提及数据库服务。
 */
public class MybatisPlusMentionDatabaseService implements MentionDatabaseService {

    private final MentionMapper mentionMapper;

    public MybatisPlusMentionDatabaseService(MentionMapper mentionMapper) {
        this.mentionMapper = mentionMapper;
    }

    @Override
    public void insert(MentionRecord record) {
        executeVoid(() -> mentionMapper.insert(toEntity(record)), "failed to insert mention");
    }

    @Override
    public List<MentionRecord> listByAccountId(long accountId, Long cursorMentionId, int limit, boolean unreadOnly, Long channelId) {
        return execute(() -> mentionMapper.listByAccountId(accountId, cursorMentionId, limit, unreadOnly, channelId).stream().map(this::toRecord).toList(), "failed to query mentions");
    }

    @Override
    public boolean markAsRead(long accountId, long mentionId) {
        return execute(() -> mentionMapper.markAsRead(accountId, mentionId) > 0, "failed to mark mention as read");
    }

    @Override
    public int markAllAsRead(long accountId, Long beforeMentionId, Long channelId) {
        return execute(() -> mentionMapper.markAllAsRead(accountId, beforeMentionId, channelId), "failed to batch mark mentions as read");
    }

    private <T> T execute(DatabaseOperation<T> operation, String errorMessage) {
        try {
            return operation.run();
        } catch (RuntimeException exception) {
            throw new DatabaseServiceException(errorMessage, exception);
        }
    }

    private MentionRecord toRecord(MentionEntity entity) {
        return new MentionRecord(entity.getMentionId(), entity.getChannelId(), entity.getMessageId(), entity.getFromAccountId(), entity.getTargetType(), entity.getTargetAccountId(), entity.getCreatedAt(), Boolean.TRUE.equals(entity.getRead()));
    }

    private MentionEntity toEntity(MentionRecord record) {
        MentionEntity entity = new MentionEntity();
        entity.setMentionId(record.mentionId());
        entity.setChannelId(record.channelId());
        entity.setMessageId(record.messageId());
        entity.setFromAccountId(record.fromAccountId());
        entity.setTargetType(record.targetType());
        entity.setTargetAccountId(record.targetAccountId());
        entity.setCreatedAt(record.createdAt());
        entity.setRead(record.read());
        return entity;
    }

    @FunctionalInterface
    private interface DatabaseOperation<T> { T run(); }

    @FunctionalInterface
    private interface VoidDatabaseOperation { void run(); }

    private void executeVoid(VoidDatabaseOperation operation, String errorMessage) {
        execute(() -> {
            operation.run();
            return null;
        }, errorMessage);
    }
}
