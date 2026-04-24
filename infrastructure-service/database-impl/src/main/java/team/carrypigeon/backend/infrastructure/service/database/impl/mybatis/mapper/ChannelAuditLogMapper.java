package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelAuditLogEntity;

/**
 * 频道审计日志 Mapper。
 * 职责：提供 chat_channel_audit_log 表的 MyBatis-Plus 访问入口。
 * 边界：仅供 database-impl 内部服务使用。
 */
@Mapper
public interface ChannelAuditLogMapper extends BaseMapper<ChannelAuditLogEntity> {
}
