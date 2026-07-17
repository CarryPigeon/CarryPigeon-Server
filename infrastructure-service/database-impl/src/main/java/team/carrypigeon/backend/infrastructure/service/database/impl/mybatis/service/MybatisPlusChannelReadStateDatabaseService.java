package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import team.carrypigeon.backend.infrastructure.service.database.api.exception.DatabaseServiceException;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelReadStateRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelUnreadRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelReadStateDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelReadStateEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelUnreadProjection;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.ChannelReadStateMapper;

/**
 * MyBatis-Plus 频道已读状态数据库服务。
 * 职责：在 database-impl 中持久化频道已读锚点与未读统计查询。
 * 边界：不承载未读业务规则，只暴露存储层读写能力。
 */
public class MybatisPlusChannelReadStateDatabaseService implements ChannelReadStateDatabaseService {

    private final ChannelReadStateMapper channelReadStateMapper;

    public MybatisPlusChannelReadStateDatabaseService(ChannelReadStateMapper channelReadStateMapper) {
        this.channelReadStateMapper = channelReadStateMapper;
    }

    /**
     * 查询账户在频道中的已读状态记录。
     */
    @Override
    public Optional<ChannelReadStateRecord> findByChannelIdAndAccountId(long channelId, long accountId) {
        return execute(() -> Optional.ofNullable(channelReadStateMapper.findByChannelIdAndAccountId(channelId, accountId)).map(this::toRecord), "failed to query channel read state");
    }

    /**
     * 新增或覆盖频道已读状态记录。
     */
    @Override
    public void upsert(ChannelReadStateRecord record) {
        executeVoid(() -> channelReadStateMapper.upsertState(toEntity(record)), "failed to upsert channel read state");
    }

    /**
     * 查询账户各频道未读统计。
     */
    @Override
    public List<ChannelUnreadRecord> listUnreadsByAccountId(long accountId) {
        return execute(() -> channelReadStateMapper.listUnreadsByAccountId(accountId).stream()
                .map(this::toUnreadRecord)
                .toList(), "failed to query channel unread counts");
    }

    private <T> T execute(DatabaseOperation<T> operation, String errorMessage) {
        try {
            return operation.run();
        } catch (DataAccessException exception) {
            throw new DatabaseServiceException(errorMessage, exception);
        } catch (RuntimeException exception) {
            throw new DatabaseServiceException(errorMessage, exception);
        }
    }

    private void executeVoid(VoidDatabaseOperation operation, String errorMessage) {
        execute(() -> {
            operation.run();
            return null;
        }, errorMessage);
    }

    private ChannelReadStateRecord toRecord(ChannelReadStateEntity entity) {
        return new ChannelReadStateRecord(
                entity.getChannelId(),
                entity.getAccountId(),
                entity.getLastReadMessageId(),
                entity.getLastReadTime(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private ChannelReadStateEntity toEntity(ChannelReadStateRecord record) {
        ChannelReadStateEntity entity = new ChannelReadStateEntity();
        entity.setChannelId(record.channelId());
        entity.setAccountId(record.accountId());
        entity.setLastReadMessageId(record.lastReadMessageId());
        entity.setLastReadTime(record.lastReadTime());
        entity.setCreatedAt(record.createdAt());
        entity.setUpdatedAt(record.updatedAt());
        return entity;
    }

    private ChannelUnreadRecord toUnreadRecord(ChannelUnreadProjection projection) {
        return new ChannelUnreadRecord(
                projection.getChannelId() == null ? 0L : projection.getChannelId(),
                projection.getUnreadCount() == null ? 0L : projection.getUnreadCount(),
                projection.getLastReadTime()
        );
    }

    /**
     * 有返回值的数据库访问操作。
     * 职责：让统一异常包装方法接收 mapper 查询或写入返回值。
     */
    @FunctionalInterface
    private interface DatabaseOperation<T> {
        T run();
    }

    /**
     * 无返回值的数据库访问操作。
     * 职责：让统一异常包装方法复用同一条数据库异常转换路径。
     */
    @FunctionalInterface
    private interface VoidDatabaseOperation {
        void run();
    }
}
