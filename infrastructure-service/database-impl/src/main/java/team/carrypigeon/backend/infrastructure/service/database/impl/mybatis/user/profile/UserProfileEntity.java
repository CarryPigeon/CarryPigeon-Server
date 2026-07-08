package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.user.profile;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import lombok.Data;
import team.carrypigeon.backend.infrastructure.service.database.api.user.profile.UserProfileRecord;

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
    private Long sex;
    private Long birthday;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * 将数据库记录契约转换为持久化实体。
     *
     * @param record 用户资料数据库记录契约
     * @return 对应持久化实体
     */
    public static UserProfileEntity fromRecord(UserProfileRecord record) {
        UserProfileEntity entity = new UserProfileEntity();
        entity.setAccountId(record.accountId());
        entity.setNickname(record.nickname());
        entity.setAvatarUrl(record.avatarUrl());
        entity.setBio(record.bio());
        entity.setSex(record.sex());
        entity.setBirthday(record.birthday());
        entity.setCreatedAt(record.createdAt());
        entity.setUpdatedAt(record.updatedAt());
        return entity;
    }

    /**
     * 将持久化实体转换为数据库记录契约。
     *
     * @return 对应数据库记录契约
     */
    public UserProfileRecord toRecord() {
        return new UserProfileRecord(
                accountId,
                nickname,
                avatarUrl,
                bio,
                sex == null ? 0L : sex,
                birthday == null ? 0L : birthday,
                createdAt,
                updatedAt
        );
    }
}
