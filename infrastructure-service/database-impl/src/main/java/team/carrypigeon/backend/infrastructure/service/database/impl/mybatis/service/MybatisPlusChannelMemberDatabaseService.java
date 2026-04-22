package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.util.List;
import org.springframework.dao.DataAccessException;
import team.carrypigeon.backend.infrastructure.service.database.api.exception.DatabaseServiceException;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelMemberRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelMemberDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.ChannelMemberMapper;

/**
 * MyBatis-Plus 频道成员数据库服务。
 * 职责：在 database-impl 中完成频道成员最小读写能力。
 * 边界：只负责数据库记录映射，不承载成员业务规则。
 */
public class MybatisPlusChannelMemberDatabaseService implements ChannelMemberDatabaseService {

    private final ChannelMemberMapper channelMemberMapper;

    public MybatisPlusChannelMemberDatabaseService(ChannelMemberMapper channelMemberMapper) {
        this.channelMemberMapper = channelMemberMapper;
    }

    @Override
    public boolean exists(long channelId, long accountId) {
        try {
            return channelMemberMapper.countMembership(channelId, accountId) > 0;
        } catch (DataAccessException exception) {
            throw new DatabaseServiceException("failed to query channel membership", exception);
        } catch (RuntimeException exception) {
            throw new DatabaseServiceException("failed to query channel membership", exception);
        }
    }

    @Override
    public void insert(ChannelMemberRecord record) {
        try {
            channelMemberMapper.insertMembership(record.channelId(), record.accountId(), record.joinedAt());
        } catch (DataAccessException exception) {
            throw new DatabaseServiceException("failed to insert channel membership", exception);
        } catch (RuntimeException exception) {
            throw new DatabaseServiceException("failed to insert channel membership", exception);
        }
    }

    @Override
    public List<Long> findAccountIdsByChannelId(long channelId) {
        try {
            return channelMemberMapper.findAccountIdsByChannelId(channelId);
        } catch (DataAccessException exception) {
            throw new DatabaseServiceException("failed to query channel member account ids", exception);
        } catch (RuntimeException exception) {
            throw new DatabaseServiceException("failed to query channel member account ids", exception);
        }
    }

}
