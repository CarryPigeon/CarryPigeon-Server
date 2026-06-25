package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.auth.session;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import lombok.Data;
import team.carrypigeon.backend.infrastructure.service.database.api.auth.session.AuthRefreshSessionRecord;

/**
 * 刷新会话持久化实体。
 * 职责：承接 auth_refresh_session 表字段与 MyBatis-Plus 映射。
 * 边界：仅供 database-impl 内部使用，不暴露到 database-api。
 */
@TableName("auth_refresh_session")
@Data
public class AuthRefreshSessionEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;
    private Long accountId;
    private String refreshTokenHash;
    private Instant expiresAt;
    private Boolean revoked;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * 将数据库记录契约转换为持久化实体。
     *
     * @param record 刷新会话数据库记录契约
     * @return 对应持久化实体
     */
    public static AuthRefreshSessionEntity fromRecord(AuthRefreshSessionRecord record) {
        AuthRefreshSessionEntity entity = new AuthRefreshSessionEntity();
        entity.setId(record.id());
        entity.setAccountId(record.accountId());
        entity.setRefreshTokenHash(record.refreshTokenHash());
        entity.setExpiresAt(record.expiresAt());
        entity.setRevoked(record.revoked());
        entity.setCreatedAt(record.createdAt());
        entity.setUpdatedAt(record.updatedAt());
        return entity;
    }

    /**
     * 将持久化实体转换为数据库记录契约。
     *
     * @return 对应数据库记录契约
     */
    public AuthRefreshSessionRecord toRecord() {
        return new AuthRefreshSessionRecord(
                id,
                accountId,
                refreshTokenHash,
                expiresAt,
                Boolean.TRUE.equals(revoked),
                createdAt,
                updatedAt
        );
    }
}
