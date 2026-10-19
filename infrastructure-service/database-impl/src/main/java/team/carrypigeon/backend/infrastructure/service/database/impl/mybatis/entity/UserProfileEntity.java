package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.infrastructure.service.database.api.model.UserProfileRecord;

/**
 * 用户资料持久化实体。
 * 职责：承接 user_profile 表字段与 MyBatis-Plus 映射。
 * 边界：仅供 database-impl 内部使用，不暴露到 database-api。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("user_profile")
public class UserProfileEntity {

    @TableId(value = "account_id", type = IdType.INPUT)
    private Long accountId;
    private String avatarUrl;
    private String bio;
    private Long sex;
    private Long birthday;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * 将持久化实体转换为数据库记录契约。
     * 约束：sex 与 birthday 列可为空，读取契约统一回退为 0。
     *
     * @return 对应数据库记录契约
     */
    public UserProfileRecord toRecord() {
        return new UserProfileRecord(
                accountId,
                avatarUrl,
                bio,
                sex == null ? 0L : sex,
                birthday == null ? 0L : birthday,
                createdAt,
                updatedAt
        );
    }

    /**
     * 将数据库记录契约转换为持久化实体。
     *
     * @param record 用户资料数据库记录契约
     * @return 对应持久化实体
     */
    public static UserProfileEntity from(UserProfileRecord record) {
        return new UserProfileEntity(
                record.accountId(),
                record.avatarUrl(),
                record.bio(),
                record.sex(),
                record.birthday(),
                record.createdAt(),
                record.updatedAt()
        );
    }
}
