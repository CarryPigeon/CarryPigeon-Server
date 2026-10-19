package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.MessageIdempotencyEntity;

/**
 * 消息幂等 Mapper。
 * 职责：利用唯一键提供事务内的原子预留入口。
 * 边界：调用必须由上层 TransactionRunner 包裹；其余读写由服务层用 MyBatis-Plus 条件构造器表达。
 */
@Mapper
public interface MessageIdempotencyMapper extends BaseMapper<MessageIdempotencyEntity> {

    /**
     * 尝试插入预留；键已存在时执行无值变更更新并等待已有事务释放唯一键锁。
     *
     * @param entity 预留实体
     * @return 受影响行数
     */
    @Insert("""
            INSERT INTO chat_message_idempotency (
                account_id,
                operation,
                idempotency_key,
                request_fingerprint,
                message_id,
                created_at,
                completed_at
            )
            VALUES (
                #{accountId},
                #{operation},
                #{idempotencyKey},
                #{requestFingerprint},
                NULL,
                #{createdAt},
                NULL
            )
            ON DUPLICATE KEY UPDATE idempotency_key = VALUES(idempotency_key)
            """)
    int reserve(MessageIdempotencyEntity entity);
}
