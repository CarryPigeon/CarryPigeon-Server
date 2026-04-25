package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import lombok.Data;

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
}
