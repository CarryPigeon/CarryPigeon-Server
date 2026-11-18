package team.carrypigeon.backend.infrastructure.service.database.impl.persistence.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import team.carrypigeon.backend.infrastructure.service.database.impl.persistence.user.UserProfileEntity;

/**
 * 用户资料 Mapper。
 * 职责：提供 user_profile 表的 MyBatis-Plus 访问入口。
 * 边界：仅供 database-impl 内部服务使用；insertAll 是通用批量写入元能力。
 */
@Mapper
public interface UserProfileMapper extends BaseMapper<UserProfileEntity> {

    /** 与文件引用更新共享用户资料行锁。 */
    @Select("SELECT * FROM user_profile WHERE account_id = #{accountId} FOR UPDATE")
    UserProfileEntity selectByAccountForUpdate(@Param("accountId") long accountId);

    /**
     * 单条 SQL 批量插入用户资料。
     *
     * @param records 待插入实体
     * @return 受影响行数
     */
    @Insert("""
            <script>
            INSERT INTO user_profile (
                account_id, avatar_url, bio, sex, birthday, created_at, updated_at
            )
            VALUES
            <foreach collection="records" item="record" separator=",">
              (
                #{record.accountId},
                #{record.avatarUrl},
                #{record.bio},
                #{record.sex},
                #{record.birthday},
                #{record.createdAt},
                #{record.updatedAt}
              )
            </foreach>
            </script>
            """)
    int insertAll(@Param("records") List<UserProfileEntity> records);
}
