package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.time.Instant;
import java.util.List;
import org.springframework.dao.DataAccessException;
import team.carrypigeon.backend.infrastructure.service.database.api.exception.DatabaseServiceException;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelAuditLogReadRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelAuditLogWriteRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelAuditLogDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelAuditLogEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.ChannelAuditLogMapper;

/**
 * MyBatis-Plus 频道审计日志数据库服务。
 * 职责：在 database-impl 中完成频道审计日志的最小追加式写入能力。
 * 边界：只负责数据库记录映射，不承载审计业务规则。
 */
public class MybatisPlusChannelAuditLogDatabaseService implements ChannelAuditLogDatabaseService {

    private final ChannelAuditLogMapper channelAuditLogMapper;

    public MybatisPlusChannelAuditLogDatabaseService(ChannelAuditLogMapper channelAuditLogMapper) {
        this.channelAuditLogMapper = channelAuditLogMapper;
    }

    @Override
    public void insert(ChannelAuditLogWriteRecord record) {
        executeVoid(() -> channelAuditLogMapper.insert(toEntity(record)), "failed to insert channel audit log");
    }

    @Override
    public List<ChannelAuditLogReadRecord> list(
            Long cursorAuditId,
            int limit,
            Long channelId,
            Long actorAccountId,
            String actionType,
            Instant fromTime,
            Instant toTime
    ) {
        return execute(() -> channelAuditLogMapper.list(cursorAuditId, limit, channelId, actorAccountId, actionType, fromTime, toTime)
                .stream()
                .map(this::toReadRecord)
                .toList(), "failed to query channel audit logs");
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

    private ChannelAuditLogEntity toEntity(ChannelAuditLogWriteRecord record) {
        ChannelAuditLogEntity entity = new ChannelAuditLogEntity();
        entity.setAuditId(record.auditId());
        entity.setChannelId(record.channelId());
        entity.setActorAccountId(record.actorAccountId());
        entity.setActionType(record.actionType());
        entity.setTargetAccountId(record.targetAccountId());
        entity.setMetadata(record.metadata());
        entity.setCreatedAt(record.createdAt());
        return entity;
    }

    private ChannelAuditLogReadRecord toReadRecord(ChannelAuditLogEntity entity) {
        return new ChannelAuditLogReadRecord(
                entity.getAuditId(),
                entity.getChannelId(),
                entity.getActorAccountId(),
                entity.getActionType(),
                entity.getMetadata(),
                entity.getCreatedAt()
        );
    }
}
