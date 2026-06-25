package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.auth.account;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import lombok.Data;
import team.carrypigeon.backend.infrastructure.service.database.api.auth.account.AuthAccountRecord;

/**
 * 鉴权账户持久化实体。
 * 职责：承接 auth_account 表字段与 MyBatis-Plus 映射。
 * 边界：仅供 database-impl 内部使用，不暴露到 database-api。
 */
@TableName("auth_account")
@Data
public class AuthAccountEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;
    private String username;
    private String passwordHash;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * 将数据库记录契约转换为持久化实体。
     *
     * @param record 鉴权账户数据库记录契约
     * @return 对应持久化实体
     */
    public static AuthAccountEntity fromRecord(AuthAccountRecord record) {
        AuthAccountEntity entity = new AuthAccountEntity();
        entity.setId(record.id());
        entity.setUsername(record.username());
        entity.setPasswordHash(record.passwordHash());
        entity.setCreatedAt(record.createdAt());
        entity.setUpdatedAt(record.updatedAt());
        return entity;
    }

    /**
     * 将持久化实体转换为数据库记录契约。
     *
     * @return 对应数据库记录契约
     */
    public AuthAccountRecord toRecord() {
        return new AuthAccountRecord(
                id,
                username,
                passwordHash,
                createdAt,
                updatedAt
        );
    }
}
