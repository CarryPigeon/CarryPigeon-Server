package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import lombok.Data;

/**
 * 用户资料持久化实体。
 * 职责：承接 user_profile 表字段与 MyBatis-Plus 映射。
 * 边界：仅供 database-impl 内部使用，不暴露到 database-api。
 */
@TableName("user_profile")
@Data
public class UserProfileEntity {

    @TableId(value = "account_id", type = IdType.INPUT)
    private Long accountId;
    private String nickname;
    private String avatarUrl;
    private String bio;
    private Instant createdAt;
    private Instant updatedAt;
}
