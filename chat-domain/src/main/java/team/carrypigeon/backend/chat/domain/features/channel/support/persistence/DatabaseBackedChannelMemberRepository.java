package team.carrypigeon.backend.chat.domain.features.channel.support.persistence;

import java.util.List;
import java.util.Optional;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelMemberRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelMemberDatabaseService;

/**
 * 基于 database-api 的频道成员仓储适配器。
 * 职责：在 channel feature 内完成成员关系模型与数据库契约模型转换。
 * 边界：不包含 SQL 与数据库驱动细节。
 */
public class DatabaseBackedChannelMemberRepository implements ChannelMemberRepository {

    private final ChannelMemberDatabaseService channelMemberDatabaseService;

    public DatabaseBackedChannelMemberRepository(ChannelMemberDatabaseService channelMemberDatabaseService) {
        this.channelMemberDatabaseService = channelMemberDatabaseService;
    }

    @Override
    public boolean exists(long channelId, long accountId) {
        return channelMemberDatabaseService.exists(channelId, accountId);
    }

    @Override
    public void save(ChannelMember channelMember) {
        channelMemberDatabaseService.insert(toRecord(channelMember));
    }

    @Override
    public Optional<ChannelMember> findByChannelIdAndAccountId(long channelId, long accountId) {
        return channelMemberDatabaseService.findByChannelIdAndAccountId(channelId, accountId)
                .map(this::toDomain);
    }

    @Override
    public void update(ChannelMember channelMember) {
        channelMemberDatabaseService.update(toRecord(channelMember));
    }

    @Override
    public void delete(long channelId, long accountId) {
        channelMemberDatabaseService.delete(channelId, accountId);
    }

    @Override
    public List<ChannelMember> findByChannelId(long channelId) {
        return channelMemberDatabaseService.findByChannelId(channelId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Long> findAccountIdsByChannelId(long channelId) {
        return channelMemberDatabaseService.findAccountIdsByChannelId(channelId);
    }

    private ChannelMember toDomain(ChannelMemberRecord record) {
        return new ChannelMember(
                record.channelId(),
                record.accountId(),
                ChannelMemberRole.valueOf(record.role()),
                record.joinedAt(),
                record.mutedUntil()
        );
    }

    private ChannelMemberRecord toRecord(ChannelMember channelMember) {
        return new ChannelMemberRecord(
                channelMember.channelId(),
                channelMember.accountId(),
                channelMember.role().name(),
                channelMember.joinedAt(),
                channelMember.mutedUntil()
        );
    }
}
