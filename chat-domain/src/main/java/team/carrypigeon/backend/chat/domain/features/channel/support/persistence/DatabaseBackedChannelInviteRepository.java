package team.carrypigeon.backend.chat.domain.features.channel.support.persistence;

import java.util.Optional;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelInvite;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelInviteStatus;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelInviteRepository;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelInviteRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelInviteDatabaseService;

/**
 * 基于 database-api 的频道邀请仓储适配器。
 * 职责：在 channel feature 内完成邀请领域模型与数据库契约模型转换。
 * 边界：不包含 SQL 与数据库驱动细节。
 */
public class DatabaseBackedChannelInviteRepository implements ChannelInviteRepository {

    private final ChannelInviteDatabaseService channelInviteDatabaseService;

    public DatabaseBackedChannelInviteRepository(ChannelInviteDatabaseService channelInviteDatabaseService) {
        this.channelInviteDatabaseService = channelInviteDatabaseService;
    }

    /**
     * 查询频道对目标账户的邀请或申请记录。
     */
    @Override
    public Optional<ChannelInvite> findByChannelIdAndInviteeAccountId(long channelId, long inviteeAccountId) {
        return channelInviteDatabaseService.findByChannelIdAndInviteeAccountId(channelId, inviteeAccountId)
                .map(this::toDomain);
    }

    /**
     * 按申请 ID 查询频道邀请记录。
     */
    @Override
    public Optional<ChannelInvite> findByChannelIdAndApplicationId(long channelId, long applicationId) {
        return channelInviteDatabaseService.findByChannelIdAndApplicationId(channelId, applicationId)
                .map(this::toDomain);
    }

    /**
     * 列出频道下全部邀请 / 申请记录。
     */
    @Override
    public java.util.List<ChannelInvite> findByChannelId(long channelId) {
        return channelInviteDatabaseService.findByChannelId(channelId).stream()
                .map(this::toDomain)
                .toList();
    }

    /**
     * 持久化新的邀请或申请记录。
     */
    @Override
    public void save(ChannelInvite channelInvite) {
        channelInviteDatabaseService.insert(toRecord(channelInvite));
    }

    /**
     * 更新既有邀请或申请记录。
     */
    @Override
    public void update(ChannelInvite channelInvite) {
        channelInviteDatabaseService.update(toRecord(channelInvite));
    }

    private ChannelInvite toDomain(ChannelInviteRecord record) {
        return new ChannelInvite(
                record.channelId(),
                record.applicationId(),
                record.inviteeAccountId(),
                record.inviterAccountId(),
                record.reason(),
                ChannelInviteStatus.valueOf(record.status()),
                record.createdAt(),
                record.respondedAt()
        );
    }

    private ChannelInviteRecord toRecord(ChannelInvite channelInvite) {
        return new ChannelInviteRecord(
                channelInvite.channelId(),
                channelInvite.applicationId(),
                channelInvite.inviteeAccountId(),
                channelInvite.inviterAccountId(),
                channelInvite.reason(),
                channelInvite.status().name(),
                channelInvite.createdAt(),
                channelInvite.respondedAt()
        );
    }
}
