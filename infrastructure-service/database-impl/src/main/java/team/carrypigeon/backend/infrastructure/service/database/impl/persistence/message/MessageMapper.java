package team.carrypigeon.backend.infrastructure.service.database.impl.persistence.message;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息 Mapper。
 * 职责：提供 canonical chat_message 表的 MyBatis-Plus 访问入口。
 * 边界：仅供 database-impl 内部服务使用；分页顺序由数据库服务的查询条件指定。
 */
@Mapper
public interface MessageMapper extends BaseMapper<MessageEntity> {

}
