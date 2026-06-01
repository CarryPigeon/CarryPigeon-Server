package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import team.carrypigeon.backend.infrastructure.service.database.api.exception.DatabaseServiceException;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelMemberRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelMemberDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelMemberEntity;
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

    /**
     * 判断成员关系是否存在。
     */
    @Override
    public boolean exists(long channelId, long accountId) {
        return execute(() -> channelMemberMapper.countMembership(channelId, accountId) > 0, "failed to query channel membership");
    }

    /**
     * 插入新的频道成员记录。
     */
    @Override
    public void insert(ChannelMemberRecord record) {
        executeVoid(() -> channelMemberMapper.insertMembership(toEntity(record)), "failed to insert channel membership");
    }

    /**
     * 查询单个成员关系记录。
     */
    @Override
    public Optional<ChannelMemberRecord> findByChannelIdAndAccountId(long channelId, long accountId) {
        return execute(() ->
                Optional.ofNullable(channelMemberMapper.findByChannelIdAndAccountId(channelId, accountId))
                        .map(this::toRecord), "failed to query channel membership");
    }

    /**
     * 更新既有成员关系记录。
     */
    @Override
    public void update(ChannelMemberRecord record) {
        executeVoid(() -> channelMemberMapper.updateMembership(toEntity(record)), "failed to update channel membership");
    }

    /**
     * 删除成员关系记录。
     */
    @Override
    public void delete(long channelId, long accountId) {
        executeVoid(() -> channelMemberMapper.deleteMembership(channelId, accountId), "failed to delete channel membership");
    }

    /**
     * 查询频道下全部成员记录。
     */
    @Override
    public List<ChannelMemberRecord> findByChannelId(long channelId) {
        return execute(() ->
                channelMemberMapper.findByChannelId(channelId).stream()
                        .map(this::toRecord)
                        .toList(), "failed to query channel members");
    }

    /**
     * 查询频道下全部成员账户 ID。
     */
    @Override
    public List<Long> findAccountIdsByChannelId(long channelId) {
        return execute(() -> channelMemberMapper.findAccountIdsByChannelId(channelId), "failed to query channel member account ids");
    }

    /**
     * 查询账户加入的频道 ID 集合。
     */
    @Override
    public List<Long> findChannelIdsByAccountId(long accountId) {
        return execute(() -> channelMemberMapper.findChannelIdsByAccountId(accountId), "failed to query account channel ids");
    }

    private <T> T execute(DatabaseOperation<T> operation, String errorMessage) {
        try {
            return operation.run();
        } catch (DataAccessException exception) {
            throw new DatabaseServiceException(errorMessage, exception);
        } catch (RuntimeException exception) {
            throw new DatabaseServiceException(errorMessage, exception);
        }
    }

    private void executeVoid(VoidDatabaseOperation operation, String errorMessage) {
        execute(() -> {
            operation.run();
            return null;
        }, errorMessage);
    }

    private ChannelMemberRecord toRecord(ChannelMemberEntity entity) {
        return new ChannelMemberRecord(
                entity.getChannelId(),
                entity.getAccountId(),
                entity.getRole(),
                entity.getJoinedAt(),
                entity.getMutedUntil()
        );
    }

    private ChannelMemberEntity toEntity(ChannelMemberRecord record) {
        ChannelMemberEntity entity = new ChannelMemberEntity();
        entity.setChannelId(record.channelId());
        entity.setAccountId(record.accountId());
        entity.setRole(record.role());
        entity.setJoinedAt(record.joinedAt());
        entity.setMutedUntil(record.mutedUntil());
        return entity;
    }

    @FunctionalInterface
    private interface DatabaseOperation<T> {

        T run();
    }

    @FunctionalInterface
    private interface VoidDatabaseOperation {

        void run();
    }

}
