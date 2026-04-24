package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import org.springframework.dao.DataAccessException;
import team.carrypigeon.backend.infrastructure.service.database.api.exception.DatabaseServiceException;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelAuditLogRecord;
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
    public void insert(ChannelAuditLogRecord record) {
        try {
            channelAuditLogMapper.insert(toEntity(record));
        } catch (DataAccessException exception) {
            throw new DatabaseServiceException("failed to insert channel audit log", exception);
        } catch (RuntimeException exception) {
            throw new DatabaseServiceException("failed to insert channel audit log", exception);
        }
    }

    private ChannelAuditLogEntity toEntity(ChannelAuditLogRecord record) {
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
}
