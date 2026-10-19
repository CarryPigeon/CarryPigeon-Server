package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.infrastructure.service.database.api.model.AuthRefreshSessionRecord;

/**
 * 刷新会话持久化实体。
 * 职责：承接 auth_refresh_session 表字段与 MyBatis-Plus 映射。
 * 边界：仅供 database-impl 内部使用，不暴露到 database-api。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("auth_refresh_session")
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

    /**
     * 将数据库记录契约转换为持久化实体。
     *
     * @param record 刷新会话数据库记录契约
     * @return 对应持久化实体
     */
    public static AuthRefreshSessionEntity from(AuthRefreshSessionRecord record) {
        return new AuthRefreshSessionEntity(
                record.id(),
                record.accountId(),
                record.refreshTokenHash(),
                record.expiresAt(),
                record.revoked(),
                record.createdAt(),
                record.updatedAt()
        );
    }
}
