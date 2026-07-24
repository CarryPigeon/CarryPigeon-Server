package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.time.Instant;
import org.springframework.dao.DataAccessException;
import team.carrypigeon.backend.infrastructure.service.database.api.exception.DatabaseServiceException;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MessageIdempotencyRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.MessageIdempotencyDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.MessageIdempotencyEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.MessageIdempotencyMapper;

/**
 * MyBatis 消息幂等数据库服务。
 * 职责：组合唯一键占位、锁定读取与结果绑定，向领域侧隐藏 MySQL 并发细节。
 * 边界：方法本身不开启事务，必须参与调用方已有事务。
 */
public class MybatisPlusMessageIdempotencyDatabaseService implements MessageIdempotencyDatabaseService {

    private final MessageIdempotencyMapper mapper;

    public MybatisPlusMessageIdempotencyDatabaseService(MessageIdempotencyMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public MessageIdempotencyRecord reserve(MessageIdempotencyRecord reservation) {
        try {
            mapper.reserve(toEntity(reservation));
            MessageIdempotencyEntity locked = mapper.findForUpdate(
                    reservation.accountId(),
                    reservation.operation(),
                    reservation.idempotencyKey()
            );
            if (locked == null) {
                throw new DatabaseServiceException("failed to lock message idempotency reservation");
            }
            return toRecord(locked);
        } catch (DataAccessException exception) {
            throw new DatabaseServiceException("failed to reserve message idempotency key", exception);
        }
    }

    @Override
    public void complete(
            long accountId,
            String operation,
            String idempotencyKey,
            String requestFingerprint,
            long messageId,
            Instant completedAt
    ) {
        try {
            int updated = mapper.complete(
                    accountId,
                    operation,
                    idempotencyKey,
                    requestFingerprint,
                    messageId,
                    completedAt
            );
            if (updated != 1) {
                throw new DatabaseServiceException("failed to complete message idempotency reservation");
            }
        } catch (DataAccessException exception) {
            throw new DatabaseServiceException("failed to complete message idempotency reservation", exception);
        }
    }

    private MessageIdempotencyEntity toEntity(MessageIdempotencyRecord record) {
        MessageIdempotencyEntity entity = new MessageIdempotencyEntity();
        entity.setAccountId(record.accountId());
        entity.setOperation(record.operation());
        entity.setIdempotencyKey(record.idempotencyKey());
        entity.setRequestFingerprint(record.requestFingerprint());
        entity.setMessageId(record.messageId());
        entity.setCreatedAt(record.createdAt());
        entity.setCompletedAt(record.completedAt());
        return entity;
    }

    private MessageIdempotencyRecord toRecord(MessageIdempotencyEntity entity) {
        return new MessageIdempotencyRecord(
                entity.getAccountId(),
                entity.getOperation(),
                entity.getIdempotencyKey(),
                entity.getRequestFingerprint(),
                entity.getMessageId(),
                entity.getCreatedAt(),
                entity.getCompletedAt()
        );
    }
}
