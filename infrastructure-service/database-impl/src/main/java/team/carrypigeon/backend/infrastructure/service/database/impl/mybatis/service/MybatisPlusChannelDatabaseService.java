package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import team.carrypigeon.backend.infrastructure.service.database.api.exception.DatabaseServiceException;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.ChannelMapper;

/**
 * MyBatis-Plus 频道数据库服务。
 * 职责：在 database-impl 中完成频道最小查询能力。
 * 边界：只负责数据库记录映射，不承载频道业务规则。
 */
public class MybatisPlusChannelDatabaseService implements ChannelDatabaseService {

    private static final String LIMIT_ONE = "LIMIT 1";
    private static final String PUBLIC_CHANNEL_TYPE = "public";
    private static final String SYSTEM_CHANNEL_TYPE = "system";

    private final ChannelMapper channelMapper;

    public MybatisPlusChannelDatabaseService(ChannelMapper channelMapper) {
        this.channelMapper = channelMapper;
    }

    @Override
    public Optional<ChannelRecord> findDefaultChannel() {
        return execute(() -> {
            ChannelEntity entity = channelMapper.selectOne(new LambdaQueryWrapper<ChannelEntity>()
                    .eq(ChannelEntity::getDefaultChannel, true)
                    .eq(ChannelEntity::getType, PUBLIC_CHANNEL_TYPE)
                    .last(LIMIT_ONE));
            return Optional.ofNullable(entity).map(this::toRecord);
        }, "failed to query default channel");
    }

    @Override
    public Optional<ChannelRecord> findSystemChannel() {
        return execute(() -> {
            ChannelEntity entity = channelMapper.selectOne(new LambdaQueryWrapper<ChannelEntity>()
                    .eq(ChannelEntity::getType, SYSTEM_CHANNEL_TYPE)
                    .last(LIMIT_ONE));
            return Optional.ofNullable(entity).map(this::toRecord);
        }, "failed to query system channel");
    }

    @Override
    public Optional<ChannelRecord> findById(long channelId) {
        return execute(() -> Optional.ofNullable(channelMapper.selectById(channelId)).map(this::toRecord), "failed to query channel by id");
    }

    @Override
    public List<ChannelRecord> discoverChannels(String keyword, Long cursorChannelId, String type, int limit) {
        return execute(() -> channelMapper.discoverChannels(keyword, cursorChannelId, type, limit).stream().map(this::toRecord).toList(), "failed to discover channels");
    }

    @Override
    public void insert(ChannelRecord record) {
        executeVoid(() -> channelMapper.insert(toEntity(record)), "failed to insert channel");
    }

    @Override
    public void update(ChannelRecord record) {
        executeVoid(() -> channelMapper.updateById(toEntity(record)), "failed to update channel");
    }

    @Override
    public void delete(long channelId) {
        executeVoid(() -> channelMapper.deleteById(channelId), "failed to delete channel");
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

    private ChannelRecord toRecord(ChannelEntity entity) {
        return new ChannelRecord(
                entity.getId(),
                entity.getConversationId(),
                entity.getName(),
                entity.getBrief(),
                entity.getAvatar(),
                entity.getType(),
                Boolean.TRUE.equals(entity.getDefaultChannel()),
                entity.getMemberCount() == null ? 0L : entity.getMemberCount(),
                Boolean.TRUE.equals(entity.getRequiresApplication()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private ChannelEntity toEntity(ChannelRecord record) {
        ChannelEntity entity = new ChannelEntity();
        entity.setId(record.id());
        entity.setConversationId(record.conversationId());
        entity.setName(record.name());
        entity.setBrief(record.brief());
        entity.setAvatar(record.avatar());
        entity.setType(record.type());
        entity.setDefaultChannel(record.defaultChannel());
        entity.setCreatedAt(record.createdAt());
        entity.setUpdatedAt(record.updatedAt());
        return entity;
    }

    @FunctionalInterface
    private interface DatabaseOperation<T> {

        T run();
    }

    @FunctionalInterface
    private interface VoidDatabaseOperation {

        void run();
    }
}
