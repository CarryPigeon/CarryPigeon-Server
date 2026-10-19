package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.time.Instant;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import team.carrypigeon.backend.infrastructure.service.database.api.exception.DatabaseServiceException;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MessageIdempotencyRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.MessageIdempotencyDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.MessageIdempotencyEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.MessageIdempotencyMapper;

/**
 * MyBatis-Plus 消息幂等数据库服务。
 * 职责：组合唯一键占位、锁定读取与结果绑定，向领域侧隐藏 MySQL 并发细节。
 * 边界：方法本身不开启事务，必须参与调用方已有事务。
 */
@Component
@ConditionalOnProperty(prefix = "cp.infrastructure.service.database", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MybatisPlusMessageIdempotencyDatabaseService implements MessageIdempotencyDatabaseService {

    private static final String LOCK_CURRENT_ROW = "FOR UPDATE";

    private final MessageIdempotencyMapper messageIdempotencyMapper;

    public MybatisPlusMessageIdempotencyDatabaseService(MessageIdempotencyMapper messageIdempotencyMapper) {
        this.messageIdempotencyMapper = messageIdempotencyMapper;
    }

    /**
     * 原子创建预留，或锁定并读取已有预留。
     * 并发约束：相同账号、操作与幂等键必须串行返回同一持久化记录。
     */
    @Override
    public MessageIdempotencyRecord reserve(MessageIdempotencyRecord reservation) {
        messageIdempotencyMapper.reserve(MessageIdempotencyEntity.from(reservation));
        MessageIdempotencyEntity locked = messageIdempotencyMapper.selectOne(
                new LambdaQueryWrapper<MessageIdempotencyEntity>()
                        .eq(MessageIdempotencyEntity::getAccountId, reservation.accountId())
                        .eq(MessageIdempotencyEntity::getOperation, reservation.operation())
                        .eq(MessageIdempotencyEntity::getIdempotencyKey, reservation.idempotencyKey())
                        .last(LOCK_CURRENT_ROW));
        if (locked == null) {
            throw new DatabaseServiceException("failed to lock message idempotency reservation");
        }
        return locked.toRecord();
    }

    /**
     * 为当前事务锁定的预留绑定消息结果。
     * 约束：指纹不一致或已完成绑定时不更新，并作为异常暴露。
     */
    @Override
    public void complete(
            long accountId,
            String operation,
            String idempotencyKey,
            String requestFingerprint,
            long messageId,
            Instant completedAt
    ) {
        int updated = messageIdempotencyMapper.update(null, new LambdaUpdateWrapper<MessageIdempotencyEntity>()
                .eq(MessageIdempotencyEntity::getAccountId, accountId)
                .eq(MessageIdempotencyEntity::getOperation, operation)
                .eq(MessageIdempotencyEntity::getIdempotencyKey, idempotencyKey)
                .eq(MessageIdempotencyEntity::getRequestFingerprint, requestFingerprint)
                .isNull(MessageIdempotencyEntity::getMessageId)
                .set(MessageIdempotencyEntity::getMessageId, messageId)
                .set(MessageIdempotencyEntity::getCompletedAt, completedAt));
        if (updated != 1) {
            throw new DatabaseServiceException("failed to complete message idempotency reservation");
        }
    }
}
