package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.infrastructure.service.database.api.model.AuthAccountRecord;

/**
 * 鉴权账户持久化实体。
 * 职责：承接 auth_account 表字段与 MyBatis-Plus 映射。
 * 边界：仅供 database-impl 内部使用，不暴露到 database-api。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("auth_account")
public class AuthAccountEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;
    private String username;
    private String passwordHash;
    private String email;
    private Instant createdAt;
    private Instant updatedAt;

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
                email,
                createdAt,
                updatedAt
        );
    }

    /**
     * 将数据库记录契约转换为持久化实体。
     *
     * @param record 鉴权账户数据库记录契约
     * @return 对应持久化实体
     */
    public static AuthAccountEntity from(AuthAccountRecord record) {
        return new AuthAccountEntity(
                record.id(),
                record.username(),
                record.passwordHash(),
                record.email(),
                record.createdAt(),
                record.updatedAt()
        );
    }
}
