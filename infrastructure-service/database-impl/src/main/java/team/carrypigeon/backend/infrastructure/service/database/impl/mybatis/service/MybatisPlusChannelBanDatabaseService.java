package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.util.Optional;
import org.springframework.dao.DataAccessException;
import team.carrypigeon.backend.infrastructure.service.database.api.exception.DatabaseServiceException;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelBanRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelBanDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelBanEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.ChannelBanMapper;

/**
 * MyBatis-Plus 频道封禁数据库服务。
 * 职责：在 database-impl 中完成频道封禁的最小读写能力。
 * 边界：只负责数据库记录映射，不承载封禁业务规则。
 */
public class MybatisPlusChannelBanDatabaseService implements ChannelBanDatabaseService {

    private final ChannelBanMapper channelBanMapper;

    public MybatisPlusChannelBanDatabaseService(ChannelBanMapper channelBanMapper) {
        this.channelBanMapper = channelBanMapper;
    }

    @Override
    public Optional<ChannelBanRecord> findByChannelIdAndBannedAccountId(long channelId, long bannedAccountId) {
        return execute(
                () -> Optional.ofNullable(channelBanMapper.findByChannelIdAndBannedAccountId(channelId, bannedAccountId))
                        .map(this::toRecord),
                "failed to query channel ban"
        );
    }

    @Override
    public void insert(ChannelBanRecord record) {
        executeVoid(() -> channelBanMapper.insert(toEntity(record)), "failed to insert channel ban");
    }

    @Override
    public void update(ChannelBanRecord record) {
        executeVoid(() -> channelBanMapper.update(toEntity(record)), "failed to update channel ban");
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
    private interface DatabaseOperation<T> {

        T run();
    }

    @FunctionalInterface
    private interface VoidDatabaseOperation {

        void run();
    }

    private ChannelBanRecord toRecord(ChannelBanEntity entity) {
        return new ChannelBanRecord(
                entity.getChannelId(),
                entity.getBannedAccountId(),
                entity.getOperatorAccountId(),
                entity.getReason(),
                entity.getExpiresAt(),
                entity.getCreatedAt(),
                entity.getRevokedAt()
        );
    }

    private ChannelBanEntity toEntity(ChannelBanRecord record) {
        ChannelBanEntity entity = new ChannelBanEntity();
        entity.setChannelId(record.channelId());
        entity.setBannedAccountId(record.bannedAccountId());
        entity.setOperatorAccountId(record.operatorAccountId());
        entity.setReason(record.reason());
        entity.setExpiresAt(record.expiresAt());
        entity.setCreatedAt(record.createdAt());
        entity.setRevokedAt(record.revokedAt());
        return entity;
    }
}
