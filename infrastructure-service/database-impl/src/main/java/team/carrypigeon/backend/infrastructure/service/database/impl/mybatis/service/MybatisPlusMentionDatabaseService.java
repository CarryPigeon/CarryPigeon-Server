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
 * 职责：在 database-impl 中提供 mention 记录的最小写入、读取与已读更新能力。
 * 边界：只负责数据库记录与实体映射，不承载提及解析规则。
 */
public class MybatisPlusMentionDatabaseService implements MentionDatabaseService {

    private final MentionMapper mentionMapper;

    public MybatisPlusMentionDatabaseService(MentionMapper mentionMapper) {
        this.mentionMapper = mentionMapper;
    }

    /**
     * 插入一条提及记录。
     */
    @Override
    public void insert(MentionRecord record) {
        executeVoid(() -> mentionMapper.insert(toEntity(record)), "failed to insert mention");
    }

    /**
     * 删除指定消息产生的全部提及记录。
     */
    @Override
    public void deleteByMessageId(long messageId) {
        executeVoid(() -> mentionMapper.deleteByMessageId(messageId), "failed to delete mentions by message");
    }

    /**
     * 查询账户的提及记录流。
     */
    @Override
    public List<MentionRecord> listByAccountId(long accountId, Long cursorMentionId, int limit, boolean unreadOnly, Long channelId) {
        return execute(() -> mentionMapper.listByAccountId(accountId, cursorMentionId, limit, unreadOnly, channelId).stream().map(this::toRecord).toList(), "failed to query mentions");
    }

    /**
     * 将单条提及标记为已读。
     * 输出：返回是否实际更新了记录。
     */
    @Override
    public boolean markAsRead(long accountId, long mentionId) {
        return execute(() -> mentionMapper.markAsRead(accountId, mentionId) > 0, "failed to mark mention as read");
    }

    /**
     * 批量标记提及为已读。
     * 输出：返回实际更新数量。
     */
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

    /**
     * 有返回值的数据库访问操作。
     * 职责：让统一异常包装方法接收 mapper 查询或写入返回值。
     */
    @FunctionalInterface
    private interface DatabaseOperation<T> { T run(); }

    /**
     * 无返回值的数据库访问操作。
     * 职责：让统一异常包装方法复用同一条数据库异常转换路径。
     */
    @FunctionalInterface
    private interface VoidDatabaseOperation { void run(); }

    private void executeVoid(VoidDatabaseOperation operation, String errorMessage) {
        execute(() -> {
            operation.run();
            return null;
        }, errorMessage);
    }
}
