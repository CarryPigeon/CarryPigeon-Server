package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelEntity;

/**
 * 频道 Mapper。
 * 职责：提供 chat_channel 表的 MyBatis-Plus 访问入口。
 * 边界：仅供 database-impl 内部服务使用；insertAll 是通用批量写入元能力。
 */
@Mapper
public interface ChannelMapper extends BaseMapper<ChannelEntity> {

    /**
     * 单条 SQL 批量插入频道。
     *
     * @param records 待插入实体
     * @return 受影响行数
     */
    @Insert("""
            <script>
            INSERT INTO chat_channel (
                channel_id, name, brief, avatar, is_system, created_at, updated_at
            )
            VALUES
            <foreach collection="records" item="record" separator=",">
              (
                #{record.channelId},
                #{record.name},
                #{record.brief},
                #{record.avatar},
                #{record.systemChannel},
                #{record.createdAt},
                #{record.updatedAt}
              )
            </foreach>
            </script>
            """)
    int insertAll(@Param("records") List<ChannelEntity> records);
}
