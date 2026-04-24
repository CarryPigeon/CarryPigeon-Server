package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

    private final ChannelMapper channelMapper;

    public MybatisPlusChannelDatabaseService(ChannelMapper channelMapper) {
        this.channelMapper = channelMapper;
    }

    @Override
    public Optional<ChannelRecord> findDefaultChannel() {
        try {
            ChannelEntity entity = channelMapper.selectOne(new LambdaQueryWrapper<ChannelEntity>()
                    .eq(ChannelEntity::getDefaultChannel, true)
                    .eq(ChannelEntity::getType, "public")
                    .last("LIMIT 1"));
            return Optional.ofNullable(entity).map(this::toRecord);
        } catch (DataAccessException exception) {
            throw new DatabaseServiceException("failed to query default channel", exception);
        } catch (RuntimeException exception) {
            throw new DatabaseServiceException("failed to query default channel", exception);
        }
    }

    @Override
    public Optional<ChannelRecord> findById(long channelId) {
        try {
            return Optional.ofNullable(channelMapper.selectById(channelId)).map(this::toRecord);
        } catch (DataAccessException exception) {
            throw new DatabaseServiceException("failed to query channel by id", exception);
        } catch (RuntimeException exception) {
            throw new DatabaseServiceException("failed to query channel by id", exception);
        }
    }

    @Override
    public void insert(ChannelRecord record) {
        try {
            channelMapper.insert(toEntity(record));
        } catch (DataAccessException exception) {
            throw new DatabaseServiceException("failed to insert channel", exception);
        } catch (RuntimeException exception) {
            throw new DatabaseServiceException("failed to insert channel", exception);
        }
    }

    private ChannelRecord toRecord(ChannelEntity entity) {
        return new ChannelRecord(
                entity.getId(),
                entity.getConversationId(),
                entity.getName(),
                entity.getType(),
                Boolean.TRUE.equals(entity.getDefaultChannel()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private ChannelEntity toEntity(ChannelRecord record) {
        ChannelEntity entity = new ChannelEntity();
        entity.setId(record.id());
        entity.setConversationId(record.conversationId());
        entity.setName(record.name());
        entity.setType(record.type());
        entity.setDefaultChannel(record.defaultChannel());
        entity.setCreatedAt(record.createdAt());
        entity.setUpdatedAt(record.updatedAt());
        return entity;
    }
}
