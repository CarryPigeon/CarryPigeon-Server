package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import team.carrypigeon.backend.infrastructure.service.database.api.exception.DatabaseServiceException;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelPinRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelPinDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelPinEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.ChannelPinMapper;

/**
 * MyBatis-Plus 频道置顶数据库服务。
 */
public class MybatisPlusChannelPinDatabaseService implements ChannelPinDatabaseService {

    private final ChannelPinMapper channelPinMapper;

    public MybatisPlusChannelPinDatabaseService(ChannelPinMapper channelPinMapper) {
        this.channelPinMapper = channelPinMapper;
    }

    @Override
    public Optional<ChannelPinRecord> findByChannelIdAndMessageId(long channelId, long messageId) {
        return execute(() -> Optional.ofNullable(channelPinMapper.findByChannelIdAndMessageId(channelId, messageId)).map(this::toRecord), "failed to query channel pin");
    }

    @Override
    public void insert(ChannelPinRecord record) {
        executeVoid(() -> channelPinMapper.insert(toEntity(record)), "failed to insert channel pin");
    }

    @Override
    public void delete(long channelId, long messageId) {
        executeVoid(() -> channelPinMapper.delete(channelId, messageId), "failed to delete channel pin");
    }

    @Override
    public List<ChannelPinRecord> findByChannelIdBefore(long channelId, Long cursorMessageId, int limit) {
        return execute(() -> channelPinMapper.findByChannelIdBefore(channelId, cursorMessageId, limit).stream().map(this::toRecord).toList(), "failed to query channel pins");
    }

    @Override
    public long countByChannelId(long channelId) {
        return execute(() -> channelPinMapper.countByChannelId(channelId), "failed to count channel pins");
    }

    private ChannelPinRecord toRecord(ChannelPinEntity entity) {
        return new ChannelPinRecord(entity.getPinId(), entity.getChannelId(), entity.getMessageId(), entity.getPinnedByAccountId(), entity.getNote(), entity.getPinnedAt());
    }

    private ChannelPinEntity toEntity(ChannelPinRecord record) {
        ChannelPinEntity entity = new ChannelPinEntity();
        entity.setPinId(record.pinId());
        entity.setChannelId(record.channelId());
        entity.setMessageId(record.messageId());
        entity.setPinnedByAccountId(record.pinnedByAccountId());
        entity.setNote(record.note());
        entity.setPinnedAt(record.pinnedAt());
        return entity;
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

    @FunctionalInterface
    private interface DatabaseOperation<T> { T run(); }

    @FunctionalInterface
    private interface VoidDatabaseOperation { void run(); }
}
