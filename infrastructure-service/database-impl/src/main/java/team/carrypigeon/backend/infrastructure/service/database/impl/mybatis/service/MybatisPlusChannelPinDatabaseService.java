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
 * 职责：在 database-impl 中完成频道置顶记录的最小读写能力。
 * 边界：只负责记录映射与异常收口，不承载置顶数量和权限规则。
 */
public class MybatisPlusChannelPinDatabaseService implements ChannelPinDatabaseService {

    private final ChannelPinMapper channelPinMapper;

    public MybatisPlusChannelPinDatabaseService(ChannelPinMapper channelPinMapper) {
        this.channelPinMapper = channelPinMapper;
    }

    /**
     * 查询指定消息的置顶记录。
     */
    @Override
    public Optional<ChannelPinRecord> findByChannelIdAndMessageId(long channelId, long messageId) {
        return execute(() -> Optional.ofNullable(channelPinMapper.findByChannelIdAndMessageId(channelId, messageId)).map(this::toRecord), "failed to query channel pin");
    }

    /**
     * 插入新的置顶记录。
     */
    @Override
    public void insert(ChannelPinRecord record) {
        executeVoid(() -> channelPinMapper.insert(toEntity(record)), "failed to insert channel pin");
    }

    /**
     * 删除置顶记录。
     */
    @Override
    public void delete(long channelId, long messageId) {
        executeVoid(() -> channelPinMapper.delete(channelId, messageId), "failed to delete channel pin");
    }

    /**
     * 删除指定消息关联的全部置顶记录。
     */
    @Override
    public void deleteByMessageId(long messageId) {
        executeVoid(() -> channelPinMapper.deleteByMessageId(messageId), "failed to delete channel pins by message");
    }

    /**
     * 查询频道内早于游标消息的置顶记录集合。
     */
    @Override
    public List<ChannelPinRecord> findByChannelIdBefore(long channelId, Long cursorMessageId, int limit) {
        return execute(() -> channelPinMapper.findByChannelIdBefore(channelId, cursorMessageId, limit).stream().map(this::toRecord).toList(), "failed to query channel pins");
    }

    /**
     * 统计频道当前置顶数量。
     */
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
}
