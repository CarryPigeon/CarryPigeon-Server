package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import team.carrypigeon.backend.infrastructure.service.database.api.exception.DatabaseServiceException;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelInviteRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelInviteDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelInviteEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.ChannelInviteMapper;

/**
 * MyBatis-Plus 频道邀请数据库服务。
 * 职责：在 database-impl 中完成频道邀请的最小读写能力。
 * 边界：只负责数据库记录映射，不承载邀请业务规则。
 */
public class MybatisPlusChannelInviteDatabaseService implements ChannelInviteDatabaseService {

    private final ChannelInviteMapper channelInviteMapper;

    public MybatisPlusChannelInviteDatabaseService(ChannelInviteMapper channelInviteMapper) {
        this.channelInviteMapper = channelInviteMapper;
    }

    /**
     * 查询频道对目标账户的邀请或申请记录。
     */
    @Override
    public Optional<ChannelInviteRecord> findByChannelIdAndInviteeAccountId(long channelId, long inviteeAccountId) {
        return execute(
                () -> Optional.ofNullable(channelInviteMapper.findByChannelIdAndInviteeAccountId(channelId, inviteeAccountId))
                        .map(this::toRecord),
                "failed to query channel invite"
        );
    }

    /**
     * 按申请 ID 查询频道邀请记录。
     */
    @Override
    public Optional<ChannelInviteRecord> findByChannelIdAndApplicationId(long channelId, long applicationId) {
        return execute(
                () -> Optional.ofNullable(channelInviteMapper.findByChannelIdAndApplicationId(channelId, applicationId))
                        .map(this::toRecord),
                "failed to query channel invite"
        );
    }

    /**
     * 查询频道下全部邀请记录。
     */
    @Override
    public List<ChannelInviteRecord> findByChannelId(long channelId) {
        return execute(
                () -> channelInviteMapper.findByChannelId(channelId).stream()
                        .map(this::toRecord)
                        .toList(),
                "failed to query channel invites"
        );
    }

    /**
     * 插入新的邀请记录。
     */
    @Override
    public void insert(ChannelInviteRecord record) {
        executeVoid(() -> channelInviteMapper.insert(toEntity(record)), "failed to insert channel invite");
    }

    /**
     * 更新既有邀请记录。
     */
    @Override
    public void update(ChannelInviteRecord record) {
        executeVoid(() -> channelInviteMapper.update(toEntity(record)), "failed to update channel invite");
    }

    private <T> T execute(DatabaseOperation<T> operation, String errorMessage) {
        try {
            return operation.run();
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

    /**
     * 有返回值的数据库访问操作。
     * 职责：让统一异常包装方法接收 mapper 查询或写入返回值。
     */
    @FunctionalInterface
    private interface DatabaseOperation<T> {

        T run();
    }

    /**
     * 无返回值的数据库访问操作。
     * 职责：让统一异常包装方法复用同一条数据库异常转换路径。
     */
    @FunctionalInterface
    private interface VoidDatabaseOperation {

        void run();
    }

    private ChannelInviteRecord toRecord(ChannelInviteEntity entity) {
        return new ChannelInviteRecord(
                entity.getChannelId(),
                entity.getApplicationId(),
                entity.getInviteeAccountId(),
                entity.getInviterAccountId(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getRespondedAt()
        );
    }

    private ChannelInviteEntity toEntity(ChannelInviteRecord record) {
        ChannelInviteEntity entity = new ChannelInviteEntity();
        entity.setChannelId(record.channelId());
        entity.setApplicationId(record.applicationId());
        entity.setInviteeAccountId(record.inviteeAccountId());
        entity.setInviterAccountId(record.inviterAccountId());
        entity.setStatus(record.status());
        entity.setCreatedAt(record.createdAt());
        entity.setRespondedAt(record.respondedAt());
        return entity;
    }
}
