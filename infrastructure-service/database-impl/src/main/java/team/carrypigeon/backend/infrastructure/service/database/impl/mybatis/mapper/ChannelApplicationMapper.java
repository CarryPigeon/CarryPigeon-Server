package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelApplicationEntity;

/**
 * 频道申请 Mapper。
 * 职责：提供 chat_channel_application 表的 MyBatis-Plus 访问入口。
 * 边界：仅供 database-impl 内部服务使用；insertAll 是通用批量写入元能力。
 */
@Mapper
public interface ChannelApplicationMapper extends BaseMapper<ChannelApplicationEntity> {

    /**
     * 单条 SQL 批量插入频道申请。
     *
     * @param records 待插入实体
     * @return 受影响行数
     */
    @Insert("""
            <script>
            INSERT INTO chat_channel_application (
                application_id, channel_id, applicant_id, reason, status, created_at, responded_at
            )
            VALUES
            <foreach collection="records" item="record" separator=",">
              (
                #{record.applicationId},
                #{record.channelId},
                #{record.applicantId},
                #{record.reason},
                #{record.status},
                #{record.createdAt},
                #{record.respondedAt}
              )
            </foreach>
            </script>
            """)
    int insertAll(@Param("records") List<ChannelApplicationEntity> records);
}
