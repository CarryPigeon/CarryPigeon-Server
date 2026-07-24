package team.carrypigeon.backend.chat.domain.features.message.support.persistence;

import java.time.Instant;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.MessageIdempotency;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageIdempotencyRepository;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MessageIdempotencyRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.MessageIdempotencyDatabaseService;

/**
 * 数据库消息幂等仓储适配器。
 * 职责：在领域幂等预留与 database-api 最小持久化投影之间转换。
 * 边界：不解释请求指纹，不实现业务冲突判断。
 */
public class DatabaseBackedMessageIdempotencyRepository implements MessageIdempotencyRepository {

    private final MessageIdempotencyDatabaseService databaseService;

    public DatabaseBackedMessageIdempotencyRepository(MessageIdempotencyDatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    @Override
    public MessageIdempotency reserve(MessageIdempotency reservation) {
        return toDomain(databaseService.reserve(toRecord(reservation)));
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
        databaseService.complete(
                accountId,
                operation,
                idempotencyKey,
                requestFingerprint,
                messageId,
                completedAt
        );
    }

    private MessageIdempotencyRecord toRecord(MessageIdempotency reservation) {
        return new MessageIdempotencyRecord(
                reservation.accountId(),
                reservation.operation(),
                reservation.idempotencyKey(),
                reservation.requestFingerprint(),
                reservation.messageId(),
                reservation.createdAt(),
                reservation.completedAt()
        );
    }

    private MessageIdempotency toDomain(MessageIdempotencyRecord record) {
        return new MessageIdempotency(
                record.accountId(),
                record.operation(),
                record.idempotencyKey(),
                record.requestFingerprint(),
                record.messageId(),
                record.createdAt(),
                record.completedAt()
        );
    }
}
