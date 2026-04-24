package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

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

    @Override
    public Optional<ChannelInviteRecord> findByChannelIdAndInviteeAccountId(long channelId, long inviteeAccountId) {
        try {
            return Optional.ofNullable(channelInviteMapper.findByChannelIdAndInviteeAccountId(channelId, inviteeAccountId))
                    .map(this::toRecord);
        } catch (DataAccessException exception) {
            throw new DatabaseServiceException("failed to query channel invite", exception);
        } catch (RuntimeException exception) {
            throw new DatabaseServiceException("failed to query channel invite", exception);
        }
    }

    @Override
    public void insert(ChannelInviteRecord record) {
        try {
            channelInviteMapper.insert(toEntity(record));
        } catch (DataAccessException exception) {
            throw new DatabaseServiceException("failed to insert channel invite", exception);
        } catch (RuntimeException exception) {
            throw new DatabaseServiceException("failed to insert channel invite", exception);
        }
    }

    @Override
    public void update(ChannelInviteRecord record) {
        try {
            channelInviteMapper.update(toEntity(record));
        } catch (DataAccessException exception) {
            throw new DatabaseServiceException("failed to update channel invite", exception);
        } catch (RuntimeException exception) {
            throw new DatabaseServiceException("failed to update channel invite", exception);
        }
    }

    private ChannelInviteRecord toRecord(ChannelInviteEntity entity) {
        return new ChannelInviteRecord(
                entity.getChannelId(),
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
        entity.setInviteeAccountId(record.inviteeAccountId());
        entity.setInviterAccountId(record.inviterAccountId());
        entity.setStatus(record.status());
        entity.setCreatedAt(record.createdAt());
        entity.setRespondedAt(record.respondedAt());
        return entity;
    }
}
